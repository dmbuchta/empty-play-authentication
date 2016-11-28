package utils;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.WithServer;

/**
 * Created by Dan on 11/23/2016.
 */
public abstract class ApplicationTest extends WithServer {

    @Override
    protected Application provideApplication() {
        return configureApp(new GuiceApplicationBuilder()).build();
    }

    public GuiceApplicationBuilder configureApp(GuiceApplicationBuilder builder) {
        return builder.configure("db.default.driver", "org.h2.Driver")
                .configure("db.default.url", "jdbc:h2:mem:play;MODE=PostgreSQL")
                .configure("jpa.default", "testPersistenceUnit");
    }

}
