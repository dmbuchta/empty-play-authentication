package services.login;

import models.User;
import org.mockito.Mock;
import play.Configuration;
import play.Logger;
import play.libs.ws.WSClient;
import repositories.UserRepository;
import services.exceptions.EnfException;
import utils.UnitTest;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;

/**
 * Created by Dan on 11/28/2016.
 */
public abstract class LoginServiceTest extends UnitTest {

    protected User user;

    @Mock
    protected UserRepository repository;
    @Mock
    protected WSClient wsClient;
    @Mock
    protected Configuration configuration;

    protected LoginService service;

    @Override
    public void setUp() {
        super.setUp();
        user = new User();
        user.setEmail("testemail@playframework.com");
        user.setPassword("password");
    }

    protected User getUserFromPromise(CompletionStage<User> promise) {
        try {
            return promise.toCompletableFuture().get();
        } catch (InterruptedException|ExecutionException e) {
            if ( e.getCause() instanceof RuntimeException)
            {
                throw (RuntimeException)e.getCause();
            }
            Logger.error("", e);
            fail("An unexpected exception was thrown");
        }
        return null;
    }
}
