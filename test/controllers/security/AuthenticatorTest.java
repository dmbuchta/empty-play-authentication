package controllers.security;

import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.mvc.Http;
import play.mvc.Result;
import services.caches.SessionCache;
import utils.UnitTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestConstants.*;

/**
 * Created by Dan on 11/30/2016.
 */
public class AuthenticatorTest extends UnitTest {

    @Mock
    private Http.Context context;
    @Mock
    private Http.Session session;
    @Mock
    private SessionCache sessionCache;
    @InjectMocks
    private Authenticator authenticator;

    @Override
    public void setUp() {
        super.setUp();
        Map<String, Object> contextArgs = new HashMap<>();
        context.args = contextArgs;
        when(context.session()).thenReturn(session);
    }

    @Test
    public void testUnauthorizedRedirect() {
        Result result = authenticator.onUnauthorized(context);
        assertEquals("Redirect was not to login page.", routes.SimpleLoginController.login().url(), result.redirectLocation().get());
    }

    @Test
    public void testGetUsername() {
        User user = new User();
        user.setEmail(FAKE_EMAIL);
        when(session.get(eq(Authenticator.SESSION_ID_PARAM))).thenReturn(FAKE_SESSION_ID);
        when(sessionCache.getUser(eq(FAKE_SESSION_ID))).thenReturn(user);

        assertEquals("Authenticator did not return the correct email address.", FAKE_EMAIL, authenticator.getUsername(context));
        verify(session).get(eq(Authenticator.SESSION_ID_PARAM));
        verify(sessionCache).getUser(eq(FAKE_SESSION_ID));
    }
}
