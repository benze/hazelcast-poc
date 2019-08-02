package cache;

import annotation.UserRolesCache;
import org.slf4j.Logger;

import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryListener;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Provider;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
@UserRolesCache
public class UserRolesEntryListenerFactory implements Factory<CacheEntryListener<String, String[]>> {
    // get a static slf4j logger for the class
    protected static final Logger logger = getLogger(UserRolesEntryListenerFactory.class);

    public UserRolesEntryListenerFactory() {
    }

    @Inject
    public UserRolesEntryListenerFactory(Provider<UserRolesCacheEntryListener> listenerProducer) {
        this.listenerProducer = listenerProducer;
    }

    private Provider<UserRolesCacheEntryListener> listenerProducer;

    @Override
    public CacheEntryListener<String, String[]> create() {
        UserRolesCacheEntryListener listener = listenerProducer.get();
        logger.info("Creating a new listener instance: {}.  Created at: {}", System.identityHashCode(listener), listener.getInstanceCreated());
        return listener;
    }
}
