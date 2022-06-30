package com.fistkim.cachesupport.repository;

import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class AbstractCacheRepository<E, K> implements CacheRepository<K, E> {

    private String cacheName;

    @Autowired
    @Qualifier("hazelcastMember")
    private HazelcastInstance hazelcastInstance;

    @Override
    public String cacheName() {
        return this.cacheName;
    }

    @Override
    public HazelcastInstance getHazelcastInstance() {
        return this.hazelcastInstance;
    }

}
