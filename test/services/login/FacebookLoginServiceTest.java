package services.login;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import play.Logger;
import play.data.Form;
import play.libs.Json;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import services.exceptions.EnfException;
import services.login.impl.FacebookLoginService;
import utils.Configs;

import javax.persistence.NoResultException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;
import static utils.TestConstants.*;

/**
 * Created by Dan on 11/28/2016.
 */
public class FacebookLoginServiceTest extends LoginServiceTest {

    protected FacebookLoginService.FacebookLoginForm loginForm;

    @Mock
    Form<FacebookLoginService.FacebookLoginForm> form;
    @Mock
    private WSRequest wsTokenRequest;
    @Mock
    private WSRequest wsEmailRequest;
    @Mock
    private CompletionStage<WSResponse> wsTokenPromise;
    @Mock
    private CompletionStage<WSResponse> wsEmailPromise;
    @Mock
    private CompletionStage<WSResponse> unusedPromise1;
    @Mock
    private CompletionStage<WSResponse> unusedPromise2;

    @Override
    public void setUp() {
        super.setUp();
        loginForm = new FacebookLoginService.FacebookLoginForm();
        loginForm.setInput_token(FAKE_INPUT_TOKEN);
        loginForm.setUserID(FAKE_USER_ID + "");
        when(form.get()).thenReturn(loginForm);

        when(configuration.getString(Configs.FB_APP_ID)).thenReturn(FAKE_APP_ID);
        when(configuration.getString(Configs.FB_APP_SECRET)).thenReturn(FAKE_APP_SECRET);
        service = new FacebookLoginService(repository, wsClient, configuration);
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Running Good Login Test");
        when(repository.findByEmail(eq(user.getEmail()))).thenReturn(user);

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID);
        data.put("is_valid", true);
        ObjectNode tokenJsonResponse = Json.newObject();
        tokenJsonResponse.set("data", data);
        mockTokenRequest(tokenJsonResponse);

        ObjectNode emailJsonResponse = Json.newObject();
        emailJsonResponse.put("email", user.getEmail());
        mockEmailRequest(emailJsonResponse);

        User userResult = getUserFromPromise(service.login(form));
        assertTrue("The user was expected to be returned", userResult.equals(user));
        verify(repository).findByEmail(eq(user.getEmail()));
    }

    @Test
    public void testGoodLoginWithNoUserAccount() {
        Logger.debug("Running Good Login Test but with no User Account");
        when(repository.findByEmail(eq(user.getEmail()))).thenThrow(new NoResultException());

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID);
        data.put("is_valid", true);
        ObjectNode tokenJsonResponse = Json.newObject();
        tokenJsonResponse.set("data", data);
        mockTokenRequest(tokenJsonResponse);

        ObjectNode emailJsonResponse = Json.newObject();
        emailJsonResponse.put("email", user.getEmail());
        mockEmailRequest(emailJsonResponse);

        try {
            getUserFromPromise(service.login(form));
        } catch (Exception e) {
            assertTrue("An Enf Exception was expected to be thrown (1)", e instanceof EnfException);
            verify(repository).findByEmail(eq(user.getEmail()));
            return;
        }
        fail("An Enf Exception was expected to be thrown (2)");
    }

    @Test
    public void testLoginWithInvalidToken() {
        Logger.debug("Testing a login with an invalid Facebook Token");

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID);
        data.put("is_valid", false);
        ObjectNode tokenJsonResponse = Json.newObject();
        mockTokenRequest(tokenJsonResponse);
        ObjectNode emailJsonResponse = Json.newObject();
        emailJsonResponse.put("email", user.getEmail());
        mockEmailRequest(emailJsonResponse);

        try {
            getUserFromPromise(service.login(form));
        } catch (Exception e) {
            assertTrue("A Runtime Exception was expected to be thrown (1)", e instanceof RuntimeException);
            assertEquals("A Runtime Exception was expected to be thrown (1)", e.getMessage(),
                    FacebookLoginService.INVALID_TOKEN_RESPONSE);
            verify(repository, never()).findByEmail(eq(user.getEmail()));
            return;
        }
        fail("A Runtime Exception was expected to be thrown (2)");
    }

    @Test
    public void testLoginWithInvalidAppId() {
        Logger.debug("Testing a login with an invalid Facebook App ID");

        ObjectNode data = Json.newObject();
        data.put("app_id", WRONG_FAKE_APP_ID);
        data.put("is_valid", true);
        ObjectNode tokenJsonResponse = Json.newObject();
        mockTokenRequest(tokenJsonResponse);
        ObjectNode emailJsonResponse = Json.newObject();
        emailJsonResponse.put("email", user.getEmail());
        mockEmailRequest(emailJsonResponse);

        try {
            getUserFromPromise(service.login(form));
        } catch (Exception e) {
            assertTrue("A Runtime Exception was expected to be thrown (1)", e instanceof RuntimeException);
            assertEquals("The Runtime Exception message is incorrect", e.getMessage(),
                    FacebookLoginService.INVALID_TOKEN_RESPONSE);
            verify(repository, never()).findByEmail(eq(user.getEmail()));
            return;
        }
        fail("A Runtime Exception was expected to be thrown (2)");
    }

    @Test
    public void testLoginWithInvalidEmailInResponse() {
        Logger.debug("Testing a login with Invalid Facebook Response (2)");

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID);
        data.put("is_valid", true);
        ObjectNode tokenJsonResponse = Json.newObject();
        tokenJsonResponse.set("data", data);
        mockTokenRequest(tokenJsonResponse);
        mockEmailRequest(Json.newObject());

        try {
            getUserFromPromise(service.login(form));
        } catch (Exception e) {
            assertTrue("A Npe Exception was expected to be thrown (1)", e instanceof NullPointerException);
            verify(repository, never()).findByEmail(eq(user.getEmail()));
            return;
        }
        fail("A Npe Exception was expected to be thrown (2)");
    }

    private void mockEmailRequest(ObjectNode json) {
        CompletableFuture<JsonNode> jsonPromise = CompletableFuture.completedFuture(json);
        ArgumentCaptor<Function> functionArgumentCaptor = ArgumentCaptor.forClass(Function.class);

        when(wsClient.url(startsWith(FacebookLoginService.FB_USER_EMAIL_ENDPOINT))).thenReturn(wsEmailRequest);
        when(wsEmailRequest.setQueryParameter(eq("fields"), eq("email"))).thenReturn(wsEmailRequest);
        when(wsEmailRequest.setQueryParameter(eq("access_token"), eq(FAKE_APP_TOKEN))).thenReturn(wsEmailRequest);
        when(wsEmailRequest.get()).thenReturn(unusedPromise2);
        when(unusedPromise2.thenApply(functionArgumentCaptor.capture())).thenReturn(wsEmailPromise);
        when(wsEmailPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(wsEmailPromise);
        when(wsEmailPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(jsonPromise);

    }

    private void mockTokenRequest(ObjectNode json) {
        CompletableFuture<JsonNode> jsonPromise = CompletableFuture.completedFuture(json);
        ArgumentCaptor<Function> functionArgumentCaptor = ArgumentCaptor.forClass(Function.class);

        when(wsClient.url(eq(FacebookLoginService.FB_TOKEN_ENDPOINT))).thenReturn(wsTokenRequest);
        when(wsTokenRequest.setQueryParameter(eq("input_token"), eq(FAKE_INPUT_TOKEN))).thenReturn(wsTokenRequest);
        when(wsTokenRequest.setQueryParameter(eq("access_token"), eq(FAKE_APP_TOKEN))).thenReturn(wsTokenRequest);
        when(wsTokenRequest.get()).thenReturn(unusedPromise1);
        when(unusedPromise1.thenApply(functionArgumentCaptor.capture())).thenReturn(wsTokenPromise);
        when(wsTokenPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(wsTokenPromise);
        when(wsTokenPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(jsonPromise);
    }
}
