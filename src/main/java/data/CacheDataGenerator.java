package data;

import annotation.UserRolesCache;
import com.hazelcast.cache.ICache;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
public class CacheDataGenerator {


    public CacheDataGenerator() {
    }

    @Inject
    public CacheDataGenerator(@UserRolesCache ICache<String, String[]> cache, HazelcastInstance hazelcastInstance) {
        this.cache = cache;
        this.hazelcastInstance = hazelcastInstance;
    }

    private ICache<String, String[]> cache;
    private HazelcastInstance hazelcastInstance;

    public void generateData(){
        IAtomicLong counter = hazelcastInstance.getAtomicLong("counter");

        while( counter.get() < 100000 ){
            String key = "" + counter.addAndGet(1);
            cache.put(key, new String[]{"asdfasdf"});
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
