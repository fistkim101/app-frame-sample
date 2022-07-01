package com.fistkim.cachesupport.configuration;

import com.hazelcast.config.MaxSizePolicy;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class HazelcastConfiguration {

    private String clusterName;
    private boolean managementEnabled;
    private Map<String, HazelcastMapSettings> mapSettings;

    @Setter
    @Getter
    public static class HazelcastMapSettings {
        private int typeId = 0;
        private int backupCount = 0;
        private int asyncBackupCount = 0;
        private int maxSize = 0;
        private MaxSizePolicy maxSizePolicy = MaxSizePolicy.PER_NODE;
        private int timeToLiveSeconds = 0;
        private String className;
        private boolean statisticsEnabled = false;
    }

}
