package com.fistkim.cachesupport.repository;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Optional;

public interface CacheRepository<K, E> {

    String cacheName();

    HazelcastInstance getHazelcastInstance();

    default IMap<K, E> getIMap() {
        return getHazelcastInstance().getMap(cacheName());
    }

    default Optional<E> findOneByKey(K key) {
        E element = this.getIMap().get(key);
        return Optional.ofNullable(element);
    }

    default E save(K key, E element) {
        return this.getIMap().put(key, element);
    }

}
