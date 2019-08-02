import annotation.UserRolesCache2;
import com.hazelcast.cache.ICache;
import com.hazelcast.core.HazelcastInstance;
import data.CacheDataGenerator;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@ApplicationScoped
public class CDIApp {

    public CDIApp() {
    }

    @Inject
    private CacheDataGenerator cacheDataGenerator;

    @Inject
    @UserRolesCache2
    private CacheDataGenerator cache2DataGenerator;

    @Inject
    @UserRolesCache2
    protected ICache<String, String[]> cache2;
    @Inject
    private HazelcastInstance hazelcastInstance;


    @Produces
    @ApplicationScoped
    @UserRolesCache2
    private CacheDataGenerator createCache2DataGenerator(@UserRolesCache2 ICache<String, String[]> cache2, HazelcastInstance hazelcastInstance){
        System.out.println("Creating generator");
        return new CacheDataGenerator(cache2, hazelcastInstance);
    }


    public void run(){
        new Thread(() -> cacheDataGenerator.generateData()).start();

        new Thread(() -> cache2DataGenerator.generateData()).start();

    }

    public static void main(String[] args) {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        CDIApp app = CDI.current().select(CDIApp.class).get();
        app.run();

//        container.shutdown();

    }
}
