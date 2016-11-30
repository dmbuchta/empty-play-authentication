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
import services.login.impl.FacebookLoginService;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static utils.TestConstants.*;
import static utils.TestUtils.parseResult;

/**
 * Created by Dan on 11/28/2016.
 */
public class FacebookLoginControllerTest extends LoginControllerTest {

    @Mock
    private Form<FacebookLoginService.FacebookLoginForm> loginForm;
    @Mock
    private JPAApi jpaApi;
    @InjectMocks
    private FacebookLoginController controller;

    @Override
    public void setUp() {
        super.setUp();
        when(formFactory.form(FacebookLoginService.FacebookLoginForm.class)).thenReturn(loginForm);
        when(loginForm.bindFromRequest()).thenReturn(loginForm);
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Testing a valid login");
        User user = new User();
        user.setId(FAKE_USER_ID);

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(CompletableFuture.completedFuture(user));
        when(session.get(eq("uId"))).thenReturn(user.getId() + "");

        Result result = getResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertTrue("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Response does not have the url key", json.has("url"));
        assertEquals("Response does not have correct url value", controllers.secured.html.routes.UserController.index().url(), json.get("url").asText());
        assertTrue("User is not being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
    }

    @Test
    public void testGoodLoginWithNoAccount() {
        Logger.debug("Testing a valid login with no account");
        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new EnfException(FAKE_EMAIL)));

        Result result = getResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "No account");
        assertTrue("Result does not have email key", json.has("email"));
        assertEquals("Result email key has the incorrect value", json.get("email").asText(), FAKE_EMAIL);
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
    }

    @Test
    public void testLoginWithBadToken() {
        Logger.debug("Testing a valid login with an invalid token");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new RuntimeException(FacebookLoginService.INVALID_TOKEN_RESPONSE)));

        Result result = getResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "There was a problem with your login");
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
    }

    @Test
    public void testLoginWithBadEmailResponse() {
        Logger.debug("Testing a valid login with an invalid email response");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new NullPointerException("No key with value 'email'")));

        Result result = getResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "There was a problem with your login");
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
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
    }

    @Test
    public void testApiGoodLogin() {
        Logger.debug("Testing a valid api login");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAccessToken(FAKE_ACCESS_TOKEN);
        refreshToken.setToken(FAKE_REFRESH_TOKEN);

        User user = new User();
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
    }

    @Test
    public void testApiGoodLoginWithNoAccount() {
        Logger.debug("Testing a valid Api login with no account");
        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new EnfException(FAKE_EMAIL)));

        Result result = getApiResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "No account");
        assertTrue("Result does not have email key", json.has("email"));
        assertEquals("Result email key has the incorrect value", json.get("email").asText(), FAKE_EMAIL);
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
    }

    @Test
    public void testApiLoginWithBadToken() {
        Logger.debug("Testing a valid Api login with an invalid token");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new RuntimeException(FacebookLoginService.INVALID_TOKEN_RESPONSE)));

        Result result = getApiResultFromController(controller);
        JsonNode json = parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "There was a problem with your login");
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
    }

    @Test
    public void testApiLoginWithBadEmailResponse() {
        Logger.debug("Testing a valid Api login with an invalid email response");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new NullPointerException("No key with value 'email'")));

        Result result = getApiResultFromController(controller);
        JsonNode json = parseResult(result);
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
        Logger.debug("Testing Api login with form errors");
        when(loginForm.hasErrors()).thenReturn(true);

        Result result = getApiResultFromController(controller);
        JsonNode json = parseResult(result);
        assertTrue("Result does not have formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());

        verify(loginService, never()).login(loginForm);
    }

    @Override
    protected Result getResultFromController(LoginController controller) {
        Result result = super.getResultFromController(controller);
        assertTrue("Result type is not json", result.contentType().toString().contains("application/json"));
        assertEquals("Result status is not OK", result.status(), OK);
        return result;
    }
}
