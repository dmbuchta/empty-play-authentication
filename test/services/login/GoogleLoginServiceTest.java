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
import services.login.impl.GoogleLoginService;
import utils.Configs;

import javax.persistence.NoResultException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static utils.TestConstants.*;

/**
 * Created by Dan on 11/28/2016.
 */
public class GoogleLoginServiceTest extends LoginServiceTest {

    protected GoogleLoginService.GoogleLoginForm loginForm;

    @Mock
    Form<GoogleLoginService.GoogleLoginForm> form;
    @Mock
    private WSRequest wsRequest;
    @Mock
    private CompletionStage<WSResponse> wsResponsePromise;
    @Mock
    private CompletionStage<WSResponse> unusedPromise;

    @Override
    public void setUp() {
        super.setUp();
        loginForm = new GoogleLoginService.GoogleLoginForm();
        loginForm.setId_token(FAKE_LOGIN_TOKEN);
        when(form.get()).thenReturn(loginForm);

        when(configuration.getString(Configs.GOOGLE_CLIENT_ID)).thenReturn(FAKE_CLIENT_ID);
        service = new GoogleLoginService(repository, wsClient, configuration);
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Running Good Login Test");
        when(repository.findByEmail(eq(user.getEmail()))).thenReturn(user);

        ObjectNode jsonResponse = Json.newObject();
        jsonResponse.put("aud", FAKE_CLIENT_ID);
        jsonResponse.put("email", user.getEmail());
        setUpWSMocking(jsonResponse);

        User userResult = getUserFromPromise(service.login(form));
        assertTrue("The user was expected to be returned", userResult.equals(user));
        verify(repository).findByEmail(eq(user.getEmail()));
    }

    @Test
    public void testGoodLoginWithNoUserAccount() {
        Logger.debug("Running Good Login Test but with no User Account");
        when(repository.findByEmail(eq(user.getEmail()))).thenThrow(new NoResultException());

        ObjectNode jsonResponse = Json.newObject();
        jsonResponse.put("aud", FAKE_CLIENT_ID);
        jsonResponse.put("email", user.getEmail());
        setUpWSMocking(jsonResponse);

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
    public void testLoginWithInvalidClientId() {
        Logger.debug("Running Login Test with an invalid Client ID in the response");

        ObjectNode jsonResponse = Json.newObject();
        jsonResponse.put("aud", WRONG_FAKE_CLIENT_ID);
        jsonResponse.put("email", user.getEmail());
        setUpWSMocking(jsonResponse);

        try {
            getUserFromPromise(service.login(form));
        } catch (Exception e) {
            assertTrue("A Runtime Exception was expected to be thrown (1)", e instanceof RuntimeException);
            assertEquals("The Runtime Exception message is incorrect", e.getMessage(),
                    GoogleLoginService.INVALID_AUD_MESSAGE);
            verify(repository, never()).findByEmail(eq(user.getEmail()));
            return;
        }
        fail("A Runtime Exception was expected to be thrown (2)");
    }


    private void setUpWSMocking(JsonNode jsonNode) {
        CompletableFuture<JsonNode> jsonPromise = CompletableFuture.completedFuture(jsonNode);
        ArgumentCaptor<Function> functionArgumentCaptor = ArgumentCaptor.forClass(Function.class);

        when(wsClient.url(GoogleLoginService.GOOGLE_TOKEN_ENDPOINT)).thenReturn(wsRequest);
        when(wsRequest.setQueryParameter(eq("id_token"), eq(FAKE_LOGIN_TOKEN))).thenReturn(wsRequest);
        when(wsRequest.get()).thenReturn(unusedPromise);
        when(unusedPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(wsResponsePromise);
        when(wsResponsePromise.thenApply(functionArgumentCaptor.capture())).thenReturn(wsResponsePromise);
        when(wsResponsePromise.thenApply(functionArgumentCaptor.capture())).thenReturn(jsonPromise);
    }

}
