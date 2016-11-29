package controllers.security;

import org.mockito.Mock;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.login.LoginService;
import utils.UnitTest;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Created by Dan on 11/28/2016.
 */
public abstract class LoginControllerTest extends UnitTest {

    @Mock
    protected Http.Context context;
    @Mock
    protected Http.Session session;
    @Mock
    protected LoginService loginService;
    @Mock
    protected FormFactory formFactory;

    @Override
    public void setUp() {
        super.setUp();
        Http.Context.current.set(context);
        when(context.session()).thenReturn(session);
    }

    protected Result getResultFromController(LoginController controller) {
        CompletionStage<Result> resultPromise = controller.login();
        try {
            return resultPromise.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            fail("An unexepceted exception was thrown");
        }
        return null;
    }
}
