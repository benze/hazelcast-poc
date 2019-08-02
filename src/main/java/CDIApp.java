import data.CacheDataGenerator;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

@ApplicationScoped
public class CDIApp {

    public CDIApp() {
    }

    @Inject
    private CacheDataGenerator cacheDataGenerator;


    public void run(){
        new Thread(() -> cacheDataGenerator.generateData()).start();
    }

    public static void main(String[] args) {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        CDIApp app = CDI.current().select(CDIApp.class).get();
        app.run();

//        container.shutdown();

    }
}
