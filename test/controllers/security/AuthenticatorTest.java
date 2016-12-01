package controllers.security;

import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.mvc.Http;
import play.mvc.Result;
import utils.UnitTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestConstants.FAKE_USER_ID;

/**
 * Created by Dan on 11/30/2016.
 */
public class AuthenticatorTest extends UnitTest {

    @Mock
    Http.Context context;
    @Mock
    Http.Session session;
    @InjectMocks
    private Authenticator authenticator;

    @Override
    public void setUp() {
        super.setUp();
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
        user.setId(FAKE_USER_ID);
        when(session.get(eq("uId"))).thenReturn(FAKE_USER_ID + "");

        assertEquals("Redirect was not to login page.", FAKE_USER_ID + "", authenticator.getUsername(context));
        verify(session).get(eq("uId"));
    }
}
