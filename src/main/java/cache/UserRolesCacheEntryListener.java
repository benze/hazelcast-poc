package cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import java.util.Date;

/**
 * Manage the deletion of UserA data on cache removal
 */
@Dependent
public class UserRolesCacheEntryListener implements CacheEntryExpiredListener<String, String[]>, CacheEntryRemovedListener<String, String[]>, CacheEntryCreatedListener<String, String[]>, HazelcastInstanceAware {
    private final static Logger LOG = LoggerFactory.getLogger(UserRolesCacheEntryListener.class);

    private HazelcastInstance hazelcastInstance;
    private Date instanceCreated = new Date();

    public Date getInstanceCreated() {
        return instanceCreated;
    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends String, ? extends String[]>> cacheEntryEvents) throws CacheEntryListenerException {
        // only the primary node will remove the key
        boolean primaryNode = hazelcastInstance.getCluster().getLocalMember() == hazelcastInstance.getCluster().getMembers().iterator().next();
        if( primaryNode )
            cacheEntryEvents.forEach(this::logEvent);
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends String, ? extends String[]>> cacheEntryEvents) throws CacheEntryListenerException {
        // only the primary node will remove the key
        boolean primaryNode = hazelcastInstance.getCluster().getLocalMember() == hazelcastInstance.getCluster().getMembers().iterator().next();
        // remove the entries
        if( primaryNode )
            cacheEntryEvents.forEach(this::logEvent);
    }

    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends String, ? extends String[]>> cacheEntryEvents) throws CacheEntryListenerException {
        cacheEntryEvents.forEach(this::logEvent);
    }

    private void logEvent(CacheEntryEvent<? extends String, ? extends String[]> cacheEntryEvent) {
        LOG.info("Event[{}:{}] on member:key [{}]:{}", cacheEntryEvent.getEventType().toString(), cacheEntryEvent.getSource().getName(), hazelcastInstance.getCluster().getLocalMember().getUuid(), cacheEntryEvent.getKey());
    }


}