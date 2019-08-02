package config;

import annotation.UserRolesCache;
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
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.slf4j.Logger;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryListener;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig.TimedExpiryPolicyFactoryConfig.ExpiryPolicyType.ACCESSED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Ensure only one instance of Hazelcast is used.
 */
@ApplicationScoped
public class HazelcastProducer {
    public static final String CACHE_NAME = "UserRoles";
    // get a static slf4j logger for the class
    protected static final Logger logger = getLogger(HazelcastProducer.class);
    private static final Logger LOG = getLogger(HazelcastProducer.class);
    private static final String HAZELCAST_ADAMS_CONFIG = "hazelcast.adams.config";
    private static final String HAZELCAST_ADAMS_CONFIG_XML = "hazelcast-poc-config.xml";

    /**
     * Produce an instance of Hazelcast configuration
     * Also used in the adams-api-rest to get have a single instance of Hazelcast.
     *
     * @return a config instance
     */
    private Config getHazelcastConfig() {
        Config hazelcastInstanceConfig = null;
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
        // assign only when hazelcast instance configured and ready to use
        return HazelcastInstanceFactory.getOrCreateHazelcastInstance(getHazelcastConfig());
    }

    /**
     * This method will create and configure the hazelcast instance
     *
     * @return a configured hazelcast instance
     */
    @Produces
    @ApplicationScoped
    @UserRolesCache
    public Cache<String, String[]> createUserRoleCache(HazelcastInstance hazelcastInstance, @UserRolesCache Factory<? extends CacheEntryListener<? super String, ? super String[]>> entryListenerFactory) {
        int ssoSessionTimeoutInSeconds = 15;

        LOG.debug("Creating cache with expiration set to {} minutes", ssoSessionTimeoutInSeconds / 60);
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

        ICache<String, String[]> cache = hazelcastInstance.getCacheManager().getCache(CACHE_NAME);
//        cache.registerCacheEntryListener(new MutableCacheEntryListenerConfiguration<>(entryListenerFactory, null, false, false));
        return cache;
    }

}
