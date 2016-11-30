package controllers.security;

import actions.CheckApiClientAction;
import org.mockito.Mock;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.login.LoginService;
import services.oauth.TokenService;
import utils.UnitTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static utils.TestConstants.FAKE_CLIENT_ID;

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
    protected TokenService tokenService;
    @Mock
    protected FormFactory formFactory;

    @Override
    public void setUp() {
        super.setUp();
        Map<String, Object> contextArgs = new HashMap<String, Object>();
        contextArgs.put(CheckApiClientAction.CLIENT_REQUEST_ARG, FAKE_CLIENT_ID);
        context.args = contextArgs;
        Http.Context.current.set(context);
        when(context.session()).thenReturn(session);
    }

    protected Result getResultFromController(LoginController controller) {
        return getResultFromPromise(controller.login());
    }

    protected Result getApiResultFromController(LoginController controller) {
        Result result = getResultFromPromise(controller.apiLogin());
        assertTrue("Result type is not json", result.contentType().toString().contains("application/json"));
        assertEquals("Result status is not OK", result.status(), OK);
        return result;
    }

    private static Result getResultFromPromise(CompletionStage<Result> stage) {
        try {
            return stage.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            fail("An unexepceted exception was thrown");
        }
        return null;
    }
}
