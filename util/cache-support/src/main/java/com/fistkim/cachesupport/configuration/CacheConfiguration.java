package com.fistkim.cachesupport.configuration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fistkim.cachesupport.support.ObjectMapperFactory;
import com.fistkim.servicecore.support.ApplicationEnvironment;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientConnectionStrategyConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.ConnectionRetryConfig;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.serialization.ByteArraySerializer;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import lombok.SneakyThrows;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class CacheConfiguration {

    private HazelcastConfiguration hazelcastConfiguration;
    private ApplicationEnvironment applicationEnvironment;
    private ObjectMapperFactory objectMapperFactory;

    public CacheConfiguration(HazelcastConfiguration hazelcastConfiguration, ApplicationEnvironment applicationEnvironment, ObjectMapperFactory objectMapperFactory) {
        this.hazelcastConfiguration = hazelcastConfiguration;
        this.applicationEnvironment = applicationEnvironment;
        this.objectMapperFactory = objectMapperFactory;
    }

//    @SneakyThrows
//    @Bean
//    public ClientConfig hazelcastClientConfig() {
//        ClientConfig clientConfig = new ClientConfig();
////        clientConfig.setProperty(Statistics.ENABLED.getName(), "true");
////        clientConfig.setProperty(ClientProperty.HEARTBEAT_TIMEOUT.getName(), "1500"); // Cache get, put 요청 타임아웃에 영향
////        clientConfig.setProperty(ClientProperty.HEARTBEAT_INTERVAL.getName(), "500");
////        clientConfig.setProperty(ClientProperty.INVOCATION_TIMEOUT_SECONDS.getName(), "2");
//
//        // 연결 재시도 부분 설정
//        ConnectionRetryConfig connectionRetryConfig = new ConnectionRetryConfig();
//        connectionRetryConfig.setInitialBackoffMillis(1 * 60 * 1000);   // 1 min
//        connectionRetryConfig.setMaxBackoffMillis(2 * 60 * 1000);       // 2 min
////        connectionRetryConfig.setEnabled(true);
//
//        ClientConnectionStrategyConfig clientConnectionStrategyConfig = new ClientConnectionStrategyConfig();
//        clientConnectionStrategyConfig.setAsyncStart(true);
//        clientConnectionStrategyConfig.setReconnectMode(ClientConnectionStrategyConfig.ReconnectMode.ASYNC);
//        clientConnectionStrategyConfig.setConnectionRetryConfig(connectionRetryConfig);
//
//        clientConfig.setConnectionStrategyConfig(clientConnectionStrategyConfig);
//
//        // network config
//        ClientNetworkConfig networkConfig = clientConfig.getNetworkConfig();
//        networkConfig.setAddresses(hazelcastSettings.getAddresses())
//                .setSmartRouting(true)
//                // .setRedoOperation(true)
//                .setConnectionTimeout(1500)
//                // .setConnectionAttemptPeriod(2000)
//                .setConnectionAttemptPeriod(2000)
//                .setConnectionAttemptLimit(7200);
//
//        // 타입별 serialization config 설정
//        Map<String, MapSettings> mapSettingsMap = hazelcastSettings.getMapSettings();
//        List<String> cacheNames = Arrays.asList(
//                ProductCacheType.PROPERTY_MAPPINGS,
//                ProductCacheType.ROOM_TYPE_MAPPINGS,
//                ProductCacheType.INTERNAL_POLICIES,
//                ProductCacheType.INTERNAL_POLICY_ATTRIBUTES,
//                ProductCacheType.INTERNAL_POLICY_ROLES,
//                ProductCacheType.PROMOTION_INVENTORIES,
//                ProductCacheType.INTERNAL_COMMON_POLICIES,
//                ProductCacheType.INTERNAL_COMMON_POLICIES_APPLY_TARGET);
//
//        for (String cacheName : cacheNames) {
//            MapSettings mapSettings = mapSettingsMap.get(cacheName);
//            if (mapSettings.getSerializerTypeId() > 0) {
//                Class<?> cls = Objects.requireNonNull(ClassUtils.getDefaultClassLoader()).loadClass(mapSettings.getClassName());
//                clientConfig.getSerializationConfig()
//                        .addSerializerConfig(new SerializerConfig()
//                                .setTypeClass(cls)
//                                .setImplementation(new HazelcastJacksonSerializer<>(hazelcastBinaryObjectMapper(), cls, mapSettings.getSerializerTypeId())));
//            }
//        }
//        return clientConfig;
//    }

    @Bean("hazelcastInstance")
    public HazelcastInstance hazelcastInstance() {
        HazelcastInstance client = HazelcastClient.newHazelcastClient(hazelcastClientConfig());
        client.getLifecycleService().addLifecycleListener(event -> log.info("Hazelcast LifecycleEvent event : {}", event));
        return client;
    }

    @Bean("hazelcastMember")
    public HazelcastInstance hazelcastMember() {
        Config memberConfiguration = this.hazelcastConfig();
        return Hazelcast.newHazelcastInstance(memberConfiguration);
    }

    @Bean
    public CacheManager cacheManager() {
        HazelcastCacheManager hazelcastCacheManager = new HazelcastCacheManager(this.hazelcastMember());
        hazelcastConfiguration.getMapSettings().keySet()
                .forEach(hazelcastCacheManager::getCache);
        return hazelcastCacheManager;
    }

    private Config hazelcastConfig() {
        String instanceName = applicationEnvironment.getApplicationName();
        String clusterName = this.hazelcastConfiguration.getClusterName();
        if (instanceName != null) {
            clusterName = instanceName;
        }

        Config configuration = new Config();
        configuration.setInstanceName(instanceName);
        configuration.setClusterName(clusterName);

        this.applyNetworkSetting(clusterName, configuration);
        this.applyMapSetting(configuration);
        this.applyManagementSetting(configuration);

        hazelcastConfiguration.getMapSettings().entrySet().stream()
                .forEach(entry -> this.customizeSerializer(entry.getValue(), configuration));

        return configuration;
    }

    private void applyNetworkSetting(String clusterName, Config configuration) {
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
                .setProperty("tag-value", environmentName + clusterName);
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
