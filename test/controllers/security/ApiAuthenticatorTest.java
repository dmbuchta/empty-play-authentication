package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.mvc.Http;
import play.mvc.Result;
import services.SessionCache;
import services.oauth.TokenService;
import utils.UnitTest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static utils.TestConstants.*;
import static utils.TestUtils.parseResult;

/**
 * Created by Dan on 11/30/2016.
 */
public class ApiAuthenticatorTest extends UnitTest {

    @Mock
    Http.Request request;
    @Mock
    Http.Context context;
    @Mock
    Http.Session session;
    @Mock
    private TokenService tokenService;
    @Mock
    private SessionCache sessionCache;
    @InjectMocks
    private ApiAuthenticator authenticator;


    @Override
    public void setUp() {
        super.setUp();
        Map<String, Object> contextArgs = new HashMap<>();
        context.args = contextArgs;
        when(context.session()).thenReturn(session);
        when(context.request()).thenReturn(request);
    }

    @Test
    public void testUnauthorizedResponseWithoutAccessToken() {
        Result result = authenticator.onUnauthorized(context);
        assertEquals("Result status is not OK", UNAUTHORIZED, result.status());

        ObjectNode json = parseResult(result);
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "You must login to perform this action.");
    }

    @Test
    public void testUnauthorizedResponseWithInvalidAccessToken() {
        try {
            Field field = ApiAuthenticator.class.getDeclaredField("isAccessTokenProvided");
            field.setAccessible(true);
            field.setBoolean(authenticator, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("No exception should be thrown");
        }

        Result result = authenticator.onUnauthorized(context);
        assertEquals("Result status is not OK", UNAUTHORIZED, result.status());

        ObjectNode json = parseResult(result);
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", "Invalid Token.", json.get("message").asText());
    }


    @Test
    public void testGetUsernameWithLoggedInUser() {
        User user = new User();
        user.setEmail(FAKE_EMAIL);
        when(session.get(eq(SESSION_ID_PARAM))).thenReturn(FAKE_SESSION_ID);
        when(sessionCache.getUser(eq(FAKE_SESSION_ID))).thenReturn(user);

        assertEquals("Username should have been returned.", FAKE_EMAIL, authenticator.getUsername(context));
        verify(tokenService, never()).getUser(anyString());
        verify(sessionCache).getUser(eq(FAKE_SESSION_ID));
    }

    @Test
    public void testGetUsernameWithoutAToken() {
        when(request.getHeader(eq(ApiAuthenticator.ACCESS_TOKEN_HEADER))).thenReturn(null);

        assertEquals("Username should not have been returned.", null, authenticator.getUsername(context));
        verify(tokenService, never()).getUser(eq(FAKE_ACCESS_TOKEN));
        verify(sessionCache, never()).getUser(anyString());
    }

    @Test
    public void testGetUsernameWithValidToken() {
        User user = new User();
        user.setEmail(FAKE_EMAIL);

        when(request.getHeader(eq(ApiAuthenticator.ACCESS_TOKEN_HEADER))).thenReturn(FAKE_ACCESS_TOKEN);
        when(tokenService.getUser(eq(FAKE_ACCESS_TOKEN))).thenReturn(user);

        assertEquals("Username should have been returned.", FAKE_EMAIL, authenticator.getUsername(context));
        verify(tokenService).getUser(eq(FAKE_ACCESS_TOKEN));
        verify(sessionCache, never()).getUser(anyString());
    }

    @Test
    public void testGetUsernameWithAnExpiredToken() {
        when(request.getHeader(eq(ApiAuthenticator.ACCESS_TOKEN_HEADER))).thenReturn(FAKE_ACCESS_TOKEN);
        when(tokenService.getUser(eq(FAKE_ACCESS_TOKEN))).thenReturn(null);

        assertEquals("Username should not have been returned.", null, authenticator.getUsername(context));
        verify(tokenService).getUser(eq(FAKE_ACCESS_TOKEN));
        verify(sessionCache, never()).getUser(anyString());
    }
}
