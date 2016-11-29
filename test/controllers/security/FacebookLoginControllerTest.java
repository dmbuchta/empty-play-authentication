package controllers.security;

import akka.dispatch.Futures;
import com.fasterxml.jackson.databind.JsonNode;
import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.Logger;
import play.data.Form;
import play.mvc.Result;
import services.exceptions.EnfException;
import services.login.impl.FacebookLoginService;
import utils.TestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;

/**
 * Created by Dan on 11/28/2016.
 */
public class FacebookLoginControllerTest extends LoginControllerTest {

    @Mock
    private Form<FacebookLoginService.FacebookLoginForm> loginForm;
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
        user.setId(100000);

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(CompletableFuture.completedFuture(user));
        when(session.get(eq("uId"))).thenReturn(user.getId() + "");

        Result result = getResultFromController(controller);
        JsonNode json = TestUtils.parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertTrue("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Response does not have the url key", json.has("url"));
        assertEquals("Response does not have correct url value", controllers.secured.routes.HomeController.index().url(), json.get("url").asText());
        assertTrue("User is not being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
    }

    @Test
    public void testGoodLoginWithNoAccount() {
        Logger.debug("Testing a valid login with no account");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new EnfException()));

        Result result = getResultFromController(controller);
        JsonNode json = TestUtils.parseResult(result);
        assertFalse("Result has formErrors key", json.has("formErrors"));
        assertTrue("Result does not have success key", json.has("success"));
        assertFalse("Result success key has the incorrect value", json.get("success").asBoolean());
        assertTrue("Result does not have message key", json.has("message"));
        assertEquals("Result message key has the incorrect value", json.get("message").asText(), "No account");
        assertFalse("User is being stored on session", Authenticator.isUserLoggedIn(context));

        verify(loginService).login(loginForm);
    }

    @Test
    public void testLoginWithBadToken() {
        Logger.debug("Testing a valid login with an invalid token");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new RuntimeException(FacebookLoginService.INVALID_TOKEN_RESPONSE)));

        Result result = getResultFromController(controller);
        JsonNode json = TestUtils.parseResult(result);
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
        Logger.debug("TTesting a valid login with an invalid email response");

        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(Futures.failedCompletionStage(new NullPointerException("No key with value 'email'")));

        Result result = getResultFromController(controller);
        JsonNode json = TestUtils.parseResult(result);
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
        JsonNode json = TestUtils.parseResult(result);
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
