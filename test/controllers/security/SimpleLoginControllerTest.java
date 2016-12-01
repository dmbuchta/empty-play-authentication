package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.RefreshToken;
import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.Configuration;
import play.Logger;
import play.data.Form;
import play.mvc.Result;
import services.AccountService;
import services.exceptions.EnfException;
import services.exceptions.InvalidTokenException;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.SEE_OTHER;
import static utils.TestConstants.*;
import static utils.TestUtils.parseResult;

/**
 * Created by Dan on 11/28/2016.
 */
public class SimpleLoginControllerTest extends LoginControllerTest {

    @Mock
    private Form<User> loginForm;
    @Mock
    private Form<SimpleLoginController.RefreshTokenForm> tokenForm;
    @Mock
    private AccountService accountService;
    @Mock
    private Configuration configuration;


    @InjectMocks
    private SimpleLoginController controller;

    @Override
    public void setUp() {
        super.setUp();
        when(formFactory.form(User.class)).thenReturn(loginForm);
        when(formFactory.form(SimpleLoginController.RefreshTokenForm.class)).thenReturn(tokenForm);
        when(loginForm.bindFromRequest()).thenReturn(loginForm);
        when(tokenForm.bindFromRequest()).thenReturn(tokenForm);
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Testing a valid login");
        User user = new User();
        user.setId(FAKE_USER_ID);
        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(CompletableFuture.completedFuture(user));
        when(session.get(eq("uId"))).thenReturn(FAKE_USER_ID + "");

        Result result = getResultFromController(controller);
        assertEquals("Did not redirect after logging in", result.status(), SEE_OTHER);
        assertEquals("Did not redirect to home page", controllers.secured.html.routes.UserController.index().url(), result.redirectLocation().get());
        assertTrue("User is not being stored on session", Authenticator.isUserLoggedIn(context));
        verify(loginService).login(loginForm);
    }

    @Test
    public void testBadLogin() {
        Logger.debug("Testing login with invalid credentials");
        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenThrow(new EnfException());

        try {
            getResultFromController(controller);
        } catch (NullPointerException npe) {
            assertTrue("Looks like a npe occurred for the wrong reasons", npe.getStackTrace()[0].toString().contains("template.scala"));
            Logger.warn("Template cannot be rendered because the form is mocked", npe);
        }
        verify(loginService).login(loginForm);
    }

    @Test
    public void testLoginWithFormErrors() {
        Logger.debug("Testing login with form errors");
        when(loginForm.hasErrors()).thenReturn(true);
        try {
            getResultFromController(controller);
        } catch (NullPointerException npe) {
            assertTrue("Looks like a npe occurred for the wrong reasons", npe.getStackTrace()[0].toString().contains("template.scala"));
            Logger.warn("Template cannot be rendered because the form is mocked", npe);
        }
        verify(loginService, never()).login(loginForm);
    }

    @Test
    public void testGoodApiLogin() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAccessToken(FAKE_ACCESS_TOKEN);
        refreshToken.setToken(FAKE_REFRESH_TOKEN);

        User user = new User();
        user.setId(FAKE_USER_ID);

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(CompletableFuture.completedFuture(user));
        when(tokenService.createRefreshToken(eq(user), eq(FAKE_CLIENT_ID))).thenReturn(refreshToken);

        ObjectNode json = parseResult(getApiResultFromController(controller));
        assertTrue("No success key", json.has("success"));
        assertTrue("Incorrect success key value", json.get("success").asBoolean());
        assertTrue("No id key", json.has("id"));
        assertTrue("Incorrect id key value", json.get("id").asLong() == FAKE_USER_ID);
        assertTrue("No REFRESH_TOKEN key", json.has(ApiAuthenticator.REFRESH_TOKEN));
        assertEquals("Incorrect REFRESH_TOKEN key value", json.get(ApiAuthenticator.REFRESH_TOKEN).asText(), refreshToken.getToken());
        assertTrue("No ACCESS_TOKEN key", json.has(ApiAuthenticator.ACCESS_TOKEN));
        assertEquals("Incorrect ACCESS_TOKEN key value", json.get(ApiAuthenticator.ACCESS_TOKEN).asText(), refreshToken.getAccessToken());

        verify(loginService).login(loginForm);
        verify(tokenService).createRefreshToken(eq(user), eq(FAKE_CLIENT_ID));
    }

    @Test
    public void testBadApiLogin() {
        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenThrow(new EnfException());

        ObjectNode json = parseResult(getApiResultFromController(controller));

        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "There was a problem with your login");
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
    }

    @Test
    public void testApiLoginWithFormErrors() {
        when(loginForm.hasErrors()).thenReturn(true);

        ObjectNode json = parseResult(getApiResultFromController(controller));
        assertTrue("Result does not have formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());

        verify(loginService, never()).login(loginForm);
    }

    @Test
    public void testTokenRefreshWithFormErrors() {
        when(tokenForm.hasErrors()).thenReturn(true);

        ObjectNode json = parseResult(controller.refreshToken());
        assertTrue("Result does not have formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());

        verify(loginService, never()).login(loginForm);
    }

    @Test
    public void testTokenRefreshWithValidToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAccessToken(FAKE_ACCESS_TOKEN_2);
        refreshToken.setToken(FAKE_REFRESH_TOKEN_2);

        SimpleLoginController.RefreshTokenForm tokenFormInput = new SimpleLoginController.RefreshTokenForm();
        tokenFormInput.setRefreshToken(FAKE_REFRESH_TOKEN);

        when(tokenForm.hasErrors()).thenReturn(false);
        when(tokenForm.get()).thenReturn(tokenFormInput);
        try {
            when(tokenService.updateRefreshToken(eq(FAKE_REFRESH_TOKEN), eq(FAKE_CLIENT_ID))).thenReturn(refreshToken);
        } catch (InvalidTokenException e) {
            e.printStackTrace();
            fail("An exception should not be thrown");
        }

        ObjectNode json = parseResult(controller.refreshToken());
        assertTrue("No success key", json.has("success"));
        assertTrue("Incorrect success key value", json.get("success").asBoolean());
        assertFalse("No id key", json.has("id"));
        assertTrue("No REFRESH_TOKEN key", json.has(ApiAuthenticator.REFRESH_TOKEN));
        assertEquals("Incorrect REFRESH_TOKEN key value", refreshToken.getToken(), json.get(ApiAuthenticator.REFRESH_TOKEN).asText());
        assertTrue("No ACCESS_TOKEN key", json.has(ApiAuthenticator.ACCESS_TOKEN));
        assertEquals("Incorrect ACCESS_TOKEN key value", refreshToken.getAccessToken(), json.get(ApiAuthenticator.ACCESS_TOKEN).asText());

        try {
            verify(tokenService).updateRefreshToken(eq(FAKE_REFRESH_TOKEN), eq(FAKE_CLIENT_ID));
        } catch (InvalidTokenException e) {
            e.printStackTrace();
            fail("An exception should not be thrown");
        }
    }

    @Test
    public void testTokenRefreshWithInvalidToken() {
        SimpleLoginController.RefreshTokenForm tokenFormInput = new SimpleLoginController.RefreshTokenForm();
        tokenFormInput.setRefreshToken(FAKE_REFRESH_TOKEN);

        when(tokenForm.hasErrors()).thenReturn(false);
        when(tokenForm.get()).thenReturn(tokenFormInput);
        try {
            when(tokenService.updateRefreshToken(eq(FAKE_REFRESH_TOKEN), eq(FAKE_CLIENT_ID))).thenThrow(new InvalidTokenException(""));
        } catch (InvalidTokenException e) {
            e.printStackTrace();
            fail("An exception should not be thrown");
        }

        ObjectNode json = parseResult(controller.refreshToken());
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", "Invalid Token.", json.get("message").asText());

        try {
            verify(tokenService).updateRefreshToken(eq(FAKE_REFRESH_TOKEN), eq(FAKE_CLIENT_ID));
        } catch (InvalidTokenException e) {
            e.printStackTrace();
            fail("An exception should not be thrown");
        }
    }

    @Test
    public void testLogout() {
        Result result = controller.logout();
        assertEquals("Logging out did not redirect to login page", routes.SimpleLoginController.showLoginPage().url(), result.redirectLocation().get());
        verify(session).remove(eq("uId"));
    }
}
