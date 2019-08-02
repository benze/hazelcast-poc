package data;

import annotation.UserRolesCache;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;

import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CacheDataGenerator {


    public CacheDataGenerator() {
    }

    @Inject
    public CacheDataGenerator(@UserRolesCache Cache<String, String[]> cache, HazelcastInstance hazelcastInstance) {
        this.cache = cache;
        this.hazelcastInstance = hazelcastInstance;
    }

    private Cache<String, String[]> cache;
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
