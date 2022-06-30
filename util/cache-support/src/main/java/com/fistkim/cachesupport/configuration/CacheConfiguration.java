package com.fistkim.cachesupport.configuration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fistkim.cachesupport.support.ObjectMapperFactory;
import com.fistkim.servicecore.support.ApplicationEnvironment;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.serialization.ByteArraySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class CacheConfiguration {

    private HazelcastConfiguration hazelcastConfiguration;
    private ApplicationEnvironment applicationEnvironment;
    private ObjectMapperFactory objectMapperFactory;

    @Bean("hazelcastMember")
    public HazelcastInstance hazelcastMember() {
        Config memberConfiguration = this.hazelcastConfig();
        return Hazelcast.newHazelcastInstance(memberConfiguration);
    }

//    @Bean
//    public CacheManager cacheManager() {
//        HazelcastCacheManager hazelcastCacheManager = new HazelcastCacheManager(this.hazelcastMember());
//        hazelcastConfiguration.getMapSettings().keySet()
//            .forEach(hazelcastCacheManager::getCache);
//        return hazelcastCacheManager;
//    }

    private Config hazelcastConfig() {
        String memberName = applicationEnvironment.getApplicationName();
        if (hazelcastConfiguration.getInstanceName() != null) {
            memberName = hazelcastConfiguration.getInstanceName();
        }

        Config configuration = new Config();
        configuration.setInstanceName(memberName);
        configuration.setClusterName(memberName);

        this.applyNetworkSetting(memberName, configuration);
        this.applyMapSetting(configuration);
        this.applyManagementSetting(configuration);

        hazelcastConfiguration.getMapSettings().entrySet().stream()
                .forEach(entry -> this.customizeSerializer(entry.getValue(), configuration));

        return configuration;
    }

    private void applyNetworkSetting(String memberName, Config configuration) {
        JoinConfig joinConfiguration = configuration.getNetworkConfig().getJoin();
        joinConfiguration.getMulticastConfig().setEnabled(false);

        if (applicationEnvironment.isLocalProfile() || applicationEnvironment.isDevelopmentProfile()) {
            joinConfiguration.getTcpIpConfig().setEnabled(true);
            joinConfiguration.getTcpIpConfig().setMembers(List.of("localhost"));
            return;
        }

        String environmentName = String.join(",", applicationEnvironment.getActiveProfiles());
        joinConfiguration.getAwsConfig()
                .setEnabled(true)
                .setProperty("region", "ap-northeast-2")
                .setProperty("use-public-ip", "false")
                .setProperty("tag-key", "RhHazelcastClusterName")
                .setProperty("tag-value", environmentName + "-hz-cluster-" + memberName);
    }

    private void applyMapSetting(Config configuration) {
        Set<String> cacheNames = hazelcastConfiguration.getMapSettings().keySet();
        if (cacheNames.isEmpty()) {
            return;
        }

        List<MapConfig> mapConfigurations = cacheNames.stream()
                .map(this::getMapConfiguration)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        mapConfigurations.forEach(configuration::addMapConfig);
    }

    private MapConfig getMapConfiguration(String cacheName) {
        HazelcastConfiguration.HazelcastMapSettings targetMap = hazelcastConfiguration.getMapSettings().get(cacheName);
        if (targetMap == null) {
            return null;
        }

        EvictionConfig evictionConfiguration = new EvictionConfig();
        evictionConfiguration.setMaxSizePolicy(targetMap.getMaxSizePolicy());
        evictionConfiguration.setSize(targetMap.getMaxSize());
        evictionConfiguration.setEvictionPolicy(EvictionPolicy.LRU);

        MapConfig mapConfiguration = new MapConfig();
        mapConfiguration.setName(cacheName);
        mapConfiguration.setEvictionConfig(evictionConfiguration);
        mapConfiguration.setBackupCount(targetMap.getBackupCount());
        mapConfiguration.setAsyncBackupCount(targetMap.getAsyncBackupCount());
        mapConfiguration.setTimeToLiveSeconds(targetMap.getTimeToLiveSeconds());

        return mapConfiguration;
    }

    private void applyManagementSetting(Config configuration) {
        if (hazelcastConfiguration.isManagementEnabled()) {
            ManagementCenterConfig managementCenterConfiguration = configuration.getManagementCenterConfig();
            managementCenterConfiguration.setScriptingEnabled(true);
            configuration.setManagementCenterConfig(managementCenterConfiguration);
        }
    }

    private void customizeSerializer(HazelcastConfiguration.HazelcastMapSettings targetMap, Config configuration) {
        try {
            Class<?> clazz = Objects.requireNonNull(ClassUtils.getDefaultClassLoader()).loadClass(targetMap.getClassName());
            int typeId = targetMap.getTypeId();

            SerializerConfig serializerConfiguration = new SerializerConfig();
            serializerConfiguration.setTypeClass(clazz);
            serializerConfiguration.setImplementation(new BinaryObjectSerializer(objectMapperFactory.getObjectMapper(new CBORFactory()), clazz, typeId));

            configuration.getSerializationConfig().addSerializerConfig(serializerConfiguration);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new RuntimeException(); // java checked exception
        }
    }

    private class BinaryObjectSerializer<T> implements ByteArraySerializer<T> {
        private final ObjectMapper objectMapper;
        private final Class<T> type;
        private final int typeId;

        public BinaryObjectSerializer(ObjectMapper cborObjectMapper, Class<T> clazz, int typeId) {
            this.objectMapper = cborObjectMapper;
            this.type = clazz;
            this.typeId = typeId;
        }

        @Override
        public byte[] write(T object) throws IOException {
            return objectMapper.writeValueAsBytes(object);
        }

        @Override
        public T read(byte[] buffer) throws IOException {
            return objectMapper.readValue(buffer, type);
        }

        @Override
        public int getTypeId() {
            return typeId;
        }
    }
}
