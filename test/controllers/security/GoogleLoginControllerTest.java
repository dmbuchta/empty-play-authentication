package controllers.security;

import akka.dispatch.Futures;
import com.fasterxml.jackson.databind.JsonNode;
import models.RefreshToken;
import models.User;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.Logger;
import play.data.Form;
import play.db.jpa.JPAApi;
import play.mvc.Result;
import services.exceptions.EnfException;
import services.login.impl.GoogleLoginService;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static utils.TestConstants.*;
import static utils.TestUtils.parseResult;

/**
 * Created by Dan on 11/28/2016.
 */
public class GoogleLoginControllerTest extends LoginControllerTest {

    @Mock
    private Form<GoogleLoginService.GoogleLoginForm> loginForm;
    @Mock
    private JPAApi jpaApi;
    @InjectMocks
    private GoogleLoginController controller;

    @Override
    public void setUp() {
        super.setUp();
        when(formFactory.form(GoogleLoginService.GoogleLoginForm.class)).thenReturn(loginForm);
        when(loginForm.bindFromRequest()).thenReturn(loginForm);
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Testing a valid login");
        User user = new User();
        user.setEmail(FAKE_EMAIL);

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(CompletableFuture.completedFuture(user));

        Result result = getResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertTrue("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Response does not have the url key", json.has("url"));
        assertEquals("Response does not have correct url value", controllers.secured.html.routes.UserController.index().url(), json.get("url").asText());

        verify(loginService).login(loginForm);
        verify(sessionCache).addUserToCache(anyString(), eq(user));
    }

    @Test
    public void testGoodLoginWithNoAccount() {
        Logger.debug("Testing a valid login with no account");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new EnfException()));

        Result result = getResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "No account");

        verify(loginService).login(loginForm);
        verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));

    }

    @Test
    public void testLoginWithBadToken() {
        Logger.debug("Testing a valid login with an invalid token");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new RuntimeException(GoogleLoginService.INVALID_AUD_MESSAGE)));

        Result result = getResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "There was a problem with your login");
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
        verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));
    }

    @Test
    public void testLoginWithFormErrors() {
        Logger.debug("Testing login with form errors");
        when(loginForm.hasErrors()).thenReturn(true);

        Result result = getResultFromController(controller);
        JsonNode json = parseResult(result);
        assertTrue("Result does not have formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());

        verify(loginService, never()).login(loginForm);
        verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));
    }

    @Test
    public void testApiGoodLogin() {
        Logger.debug("Testing a valid Api login");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAccessToken(FAKE_ACCESS_TOKEN);
        refreshToken.setToken(FAKE_REFRESH_TOKEN);

        User user = new User();
        user.setEmail(FAKE_EMAIL);
        user.setId(FAKE_USER_ID);

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(CompletableFuture.completedFuture(user));
        ArgumentCaptor<Supplier> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        when(jpaApi.withTransaction(supplierArgumentCaptor.capture())).thenReturn(refreshToken);


        Result result = getApiResultFromController(controller);
        JsonNode json = parseResult(result);
        assertTrue("No success key", json.has("success"));
        assertTrue("Incorrect success key value", json.get("success").asBoolean());
        assertTrue("No id key", json.has("id"));
        assertTrue("Incorrect id key value", json.get("id").asLong() == FAKE_USER_ID);
        assertTrue("No REFRESH_TOKEN key", json.has(ApiAuthenticator.REFRESH_TOKEN));
        assertEquals("Incorrect REFRESH_TOKEN key value", json.get(ApiAuthenticator.REFRESH_TOKEN).asText(), refreshToken.getToken());
        assertTrue("No ACCESS_TOKEN key", json.has(ApiAuthenticator.ACCESS_TOKEN));
        assertEquals("Incorrect ACCESS_TOKEN key value", json.get(ApiAuthenticator.ACCESS_TOKEN).asText(), refreshToken.getAccessToken());
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
        verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));
    }

    @Test
    public void testApiGoodLoginWithNoAccount() {
        Logger.debug("Testing a valid Api login with no account");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new EnfException()));

        Result result = getApiResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "No account");
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
        verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));
    }

    @Test
    public void testApiLoginWithBadToken() {
        Logger.debug("Testing a valid Api login with an invalid token");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new RuntimeException(GoogleLoginService.INVALID_AUD_MESSAGE)));

        Result result = getApiResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "There was a problem with your login");
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
        verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));
    }

    @Test
    public void testApiLoginWithFormErrors() {
        Logger.debug("Testing Api login with form errors");
        when(loginForm.hasErrors()).thenReturn(true);

        Result result = getApiResultFromController(controller);
        JsonNode json = parseResult(result);
        assertTrue("Result does not have formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        verify(loginService, never()).login(loginForm);
        verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));
    }

    @Override
    protected Result getResultFromController(LoginController controller) {
        Result result = super.getResultFromController(controller);
        assertTrue("Result type is not json", result.contentType().toString().contains("application/json"));
        assertEquals("Result status is not OK", result.status(), OK);
        return result;
    }
}
