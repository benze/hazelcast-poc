import cache.UserRolesCacheEntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ISemaphore;
import config.HazelcastProducer;
import org.slf4j.Logger;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryListener;

import static org.slf4j.LoggerFactory.getLogger;

public class MultiListenerTest {
    // get a static slf4j logger for the class
    protected static final Logger logger = getLogger(MultiListenerTest.class);

    public static void main(String[] args) {
        HazelcastProducer hazelcastProducer = new HazelcastProducer();
        HazelcastInstance hazelcastInstance =hazelcastProducer.getHazelcastInstance();

        Cache cache = hazelcastProducer.createUserRoleCache(hazelcastInstance,
                new Factory<CacheEntryListener<? super String, ? super String[]>>() {
                    @Override
                    public CacheEntryListener<? super String, ? super String[]> create() {
                        logger.info("Creating the lister");
                        return new UserRolesCacheEntryListener();
                    }
                }
        );


        IAtomicLong counter = hazelcastInstance.getAtomicLong("counter");

        Runnable task = () ->{
            while( counter.get() < 100000 ){
                String key = "" + counter.addAndGet(1);
                cache.put(key, new String[]{"asdfasdf"});
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                cache.remove(key);
            }
        };


        // only launch the thread on a single node
        ISemaphore semaphore = hazelcastInstance.getSemaphore( MultiListenerTest.class.getName());
        semaphore.init(1);

        try {
            semaphore.acquire();
            logger.info( "Populating cache");
            task.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            semaphore.release();
        }

//        logger.info("Launching thread to populate cache");
//            Thread thread = new Thread(task);
//            thread.start();
//
//        } else {
//            logger.info("Launching not the first node so just listen for changes");
//        }
    }
}
