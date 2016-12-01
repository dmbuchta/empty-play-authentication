package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.mvc.Http;
import play.mvc.Result;
import services.oauth.TokenService;
import utils.UnitTest;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static utils.TestConstants.FAKE_ACCESS_TOKEN;
import static utils.TestConstants.FAKE_USER_ID;
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
    @InjectMocks
    private ApiAuthenticator authenticator;


    @Override
    public void setUp() {
        super.setUp();
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
        user.setId(FAKE_USER_ID);
        when(session.get(eq("uId"))).thenReturn(FAKE_USER_ID + "");

        assertEquals("Username should have been returned.", "1", authenticator.getUsername(context));
        verify(tokenService, never()).isValidAccessToken(anyString());
    }

    @Test
    public void testGetUsernameWithoutAToken() {
        when(request.getHeader(eq(ApiAuthenticator.ACCESS_TOKEN_HEADER))).thenReturn(null);

        assertEquals("Username should not have been returned.", null, authenticator.getUsername(context));
        verify(tokenService, never()).isValidAccessToken(eq(FAKE_ACCESS_TOKEN));
    }

    @Test
    public void testGetUsernameWithValidToken() {
        when(request.getHeader(eq(ApiAuthenticator.ACCESS_TOKEN_HEADER))).thenReturn(FAKE_ACCESS_TOKEN);
        when(tokenService.isValidAccessToken(eq(FAKE_ACCESS_TOKEN))).thenReturn(true);

        assertEquals("Username should have been returned.", "1", authenticator.getUsername(context));
        verify(tokenService).isValidAccessToken(eq(FAKE_ACCESS_TOKEN));
    }

    @Test
    public void testGetUsernameWithAnExpiredToken() {
        when(request.getHeader(eq(ApiAuthenticator.ACCESS_TOKEN_HEADER))).thenReturn(FAKE_ACCESS_TOKEN);
        when(tokenService.isValidAccessToken(eq(FAKE_ACCESS_TOKEN))).thenReturn(false);

        assertEquals("Username should not have been returned.", null, authenticator.getUsername(context));
        verify(tokenService).isValidAccessToken(eq(FAKE_ACCESS_TOKEN));
    }
}
