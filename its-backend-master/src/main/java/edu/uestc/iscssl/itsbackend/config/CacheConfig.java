package edu.uestc.iscssl.itsbackend.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 缓存
 */
@Component
public class CacheConfig {

    private static CacheConfig instance;

    private static Cache<String, String> localCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(5, TimeUnit.MINUTES).build();

    public static CacheConfig getInstance(){
        if(null == instance) {
            synchronized (CacheConfig.class) {
                if (null == instance) {
                    instance = new CacheConfig();
                }
            }
        }
        return instance;
    }

    public void save(String k, String v){
        localCache.put(k, v);
    }

    public String getValue(String key){
        return localCache.getIfPresent(key);
    }

    public void remove(String key){
        String value = localCache.getIfPresent(key);
        if(!StringUtils.isEmpty(value)){
            localCache.invalidate(key);
        }
    }
}
