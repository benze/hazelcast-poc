package config;

import annotation.UserRolesCache;
import annotation.UserRolesCache2;
import com.hazelcast.cache.ICache;
import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig;
import com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig.DurationConfig;
import com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig.TimedExpiryPolicyFactoryConfig;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.slf4j.Logger;

import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryListener;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig.TimedExpiryPolicyFactoryConfig.ExpiryPolicyType.ACCESSED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Ensure only one instance of Hazelcast is used.
 */
@ApplicationScoped
public class HazelcastProducer {
    // get a static slf4j logger for the class
    protected static final Logger logger = getLogger(HazelcastProducer.class);

    public static final String CACHE_NAME = "UserRoles";
    private static final Logger LOG = getLogger(HazelcastProducer.class);
    private static final String HAZELCAST_ADAMS_CONFIG = "hazelcast.adams.config";
    private static final String HAZELCAST_ADAMS_CONFIG_XML = "hazelcast-poc-config.xml";
    private static final String ADD_CACHE_CONFIG_LOCK = "ADD_CACHE_CONFIG_LOCK";

    private static Config hazelcastInstanceConfig;
    private static HazelcastInstance hazelcastInstance;

    private static Object configLock = new Object();
    private static Object instanceLock = new Object();

    /**
     * Produce an instance of Hazelcast configuration
     * Also used in the adams-api-rest to get have a single instance of Hazelcast.
     *
     * @return a config instance
     */
    static private Config getHazelcastConfig() {
        if (hazelcastInstanceConfig == null) {
            synchronized (configLock) {
                if (hazelcastInstanceConfig == null) {
                    String configFileName = System.getProperty(HAZELCAST_ADAMS_CONFIG);
                    if (configFileName != null) {
                        try {
                            LOG.info(
                                    "Try to load hazelcast config from system property : {} file: {}",
                                    HAZELCAST_ADAMS_CONFIG,
                                    configFileName);
                            hazelcastInstanceConfig = new FileSystemXmlConfig(configFileName);
                        } catch (FileNotFoundException e) {
                            LOG.warn("Cannot find config file  : ");
                        }
                    }

                    if (hazelcastInstanceConfig == null) {
                        LOG.info("Try to load hazelcast config from classpath : {}", HAZELCAST_ADAMS_CONFIG_XML);
                        hazelcastInstanceConfig = new ClasspathXmlConfig(HAZELCAST_ADAMS_CONFIG_XML);
                    }

                    hazelcastInstanceConfig.setInstanceName("hazelcast-adams-adams");
                }
            }
        }
        return hazelcastInstanceConfig;
    }

    /**
     * Produce an Hazelcast instance
     *
     * @return an hazelcast instance
     */
    @Produces
    @ApplicationScoped
    public HazelcastInstance getHazelcastInstance() {
        if (hazelcastInstance == null) {
            synchronized (instanceLock) {
                if (hazelcastInstance == null) {
                    // assign only when hazelcast instance configured and ready to use
                    hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(getHazelcastConfig());
                }
            }
        }
        return hazelcastInstance;
    }

    /**
     * This method will create and configure the hazelcast instance
     *
     * @return a configured hazelcast instance
     */
    @Produces
    @ApplicationScoped
    @UserRolesCache
    public ICache<String, String[]> getUserRoleCache(HazelcastInstance hazelcastInstance, @UserRolesCache Factory<? extends CacheEntryListener<? super String, ? super String[]>> entryListenerFactory ){
        int ssoSessionTimeoutInSeconds = 15;

        LOG.debug("Creating ADAMS cache with expiration set to {} minutes", ssoSessionTimeoutInSeconds / 60);
        ILock addCacheConfigLock = hazelcastInstance.getLock(ADD_CACHE_CONFIG_LOCK);
        // LOCKING to prevent multiple instances of ADAMS to add the cache config at the same time.
        addCacheConfigLock.lock();
        if (hazelcastInstance.getConfig().findCacheConfigOrNull(CACHE_NAME) == null) {
            // Add cache used by adams
            CacheSimpleConfig cacheSimpleConfig = new CacheSimpleConfig()
                    .setName(CACHE_NAME)
                    .setKeyType(String.class.getName())
                    .setValueType((new String[0]).getClass().getName())
                    .setStatisticsEnabled(false)
                    .setManagementEnabled(false)
                    .setReadThrough(true)
                    .setWriteThrough(true)
                    .setInMemoryFormat(InMemoryFormat.OBJECT)
                    .setBackupCount(1)
                    .setAsyncBackupCount(1)
                    .setEvictionConfig(new EvictionConfig()
                            .setEvictionPolicy(EvictionPolicy.LRU)
                            .setSize(1000)
                            .setMaximumSizePolicy(EvictionConfig.MaxSizePolicy.ENTRY_COUNT))
                    .setExpiryPolicyFactoryConfig(
                            new ExpiryPolicyFactoryConfig(
                                    new TimedExpiryPolicyFactoryConfig(ACCESSED,
                                            new DurationConfig(
                                                    ssoSessionTimeoutInSeconds,
                                                    TimeUnit.SECONDS))));

            hazelcastInstance.getConfig().addCacheConfig(cacheSimpleConfig);
        }
        addCacheConfigLock.unlock();

        ICache<String, String[]> userRolesCache = hazelcastInstance.getCacheManager().getCache(CACHE_NAME);

        MutableCacheEntryListenerConfiguration<String, String[]> listenerConfiguration =
                new MutableCacheEntryListenerConfiguration<>(entryListenerFactory, null, false, false);

        userRolesCache.registerCacheEntryListener(listenerConfiguration);
        return userRolesCache;
    }

    @Produces
    @ApplicationScoped
    @UserRolesCache2
    public ICache<String, String[]> getUserRoleCache2(HazelcastInstance hazelcastInstance, @UserRolesCache Factory<? extends CacheEntryListener<? super String, ? super String[]>> entryListenerFactory ){
        int ssoSessionTimeoutInSeconds = 15;

        LOG.debug("Creating ADAMS cache with expiration set to {} minutes", ssoSessionTimeoutInSeconds / 60);
        ILock addCacheConfigLock = hazelcastInstance.getLock(ADD_CACHE_CONFIG_LOCK);
        // LOCKING to prevent multiple instances of ADAMS to add the cache config at the same time.
        addCacheConfigLock.lock();
        String cacheName = CACHE_NAME + "2";
        if (hazelcastInstance.getConfig().findCacheConfigOrNull(cacheName) == null) {
            // Add cache used by adams
            CacheSimpleConfig cacheSimpleConfig = new CacheSimpleConfig()
                    .setName(cacheName)
                    .setKeyType(String.class.getName())
                    .setValueType((new String[0]).getClass().getName())
                    .setStatisticsEnabled(false)
                    .setManagementEnabled(false)
                    .setReadThrough(true)
                    .setWriteThrough(true)
                    .setInMemoryFormat(InMemoryFormat.OBJECT)
                    .setBackupCount(1)
                    .setAsyncBackupCount(1)
                    .setEvictionConfig(new EvictionConfig()
                            .setEvictionPolicy(EvictionPolicy.LRU)
                            .setSize(1000)
                            .setMaximumSizePolicy(EvictionConfig.MaxSizePolicy.ENTRY_COUNT))
                    .setExpiryPolicyFactoryConfig(
                            new ExpiryPolicyFactoryConfig(
                                    new TimedExpiryPolicyFactoryConfig(ACCESSED,
                                            new DurationConfig(
                                                    ssoSessionTimeoutInSeconds,
                                                    TimeUnit.SECONDS))));

            hazelcastInstance.getConfig().addCacheConfig(cacheSimpleConfig);
        }
        addCacheConfigLock.unlock();

        ICache<String, String[]> userRolesCache = hazelcastInstance.getCacheManager().getCache(cacheName);

        MutableCacheEntryListenerConfiguration<String, String[]> listenerConfiguration =
                new MutableCacheEntryListenerConfiguration<>(entryListenerFactory, null, false, false);

        userRolesCache.registerCacheEntryListener(listenerConfiguration);
        return userRolesCache;
    }

}
