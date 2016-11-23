package utils;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.WithServer;

/**
 * Created by Dan on 11/23/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class ApplicationTest extends WithServer {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .configure("db.default.driver", "org.h2.Driver")
                .configure("db.default.url", "jdbc:h2:mem:play;MODE=PostgreSQL")
                .configure("jpa.default", "testPersistenceUnit")
                .build();
    }
}
