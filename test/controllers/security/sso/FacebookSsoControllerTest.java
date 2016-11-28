package controllers.security.sso;

import actions.CheckFacebookConfigAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import play.mvc.Result;
import services.UserService;
import services.exceptions.EnfException;
import utils.UnitTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static utils.TestUtils.parseResult;

/**
 * Created by Dan on 11/27/2016.
 */
public class FacebookSsoControllerTest extends UnitTest {

    private static final String FB_TOKEN_ENDPOINT = "https://graph.facebook.com/debug_token";
    private static final String FB_USER_EMAIL_ENDPOINT = "https://graph.facebook.com/v2.8/";

    private static final String FAKE_ACCESS_TOKEN = "THIS_IS_A_FAKE_FB_ACCESS_TOKEN";
    private static final String FAKE_APP_ID = "THIS_IS_A_FAKE_FB_APP_ID";
    private static final String FAKE_INPUT_TOKEN = "FAKE_INPUT_TOKEN";
    private static final String FAKE_USER_ID = "FAKE_USER_ID";
    private static final String FAKE_USER_EMAIL = "testemail@playframework.com";

    private FacebookSsoController controller;


    @Mock
    private JPAApi jpaApi;
    @Mock
    private FormFactory formFactory;
    @Mock
    private UserService userService;
    @Mock
    private WSClient wsClient;
    @Mock
    private Form<FacebookSsoController.FbSsoForm> ssoForm;
    @Mock
    private FacebookSsoController.FbSsoForm ssoInfo;
    @Mock
    private Http.Context context;
    @Mock
    private Http.Session session;
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
        Map<String, Object> contextArgs = new HashMap<>();
        contextArgs.put(CheckFacebookConfigAction.ACCESS_TOKEN, FAKE_ACCESS_TOKEN);
        contextArgs.put(CheckFacebookConfigAction.APP_ID, FAKE_APP_ID);
        context.args = contextArgs;

        Http.Context.current.set(context);
        when(context.session()).thenReturn(session);
        when(formFactory.form(FacebookSsoController.FbSsoForm.class)).thenReturn(ssoForm);
        when(ssoForm.bindFromRequest()).thenReturn(ssoForm);
        when(ssoForm.get()).thenReturn(ssoInfo);
        when(ssoInfo.getInput_token()).thenReturn(FAKE_INPUT_TOKEN);
        when(ssoInfo.getUserID()).thenReturn(FAKE_USER_ID);
        controller = new FacebookSsoController(jpaApi, formFactory, userService, wsClient);
    }

    @Test
    public void testLoginWithErrors() {
        Logger.debug("Testing login with form errors");

        when(ssoForm.hasErrors()).thenReturn(true);
        Result result = getResultFromController();

        ObjectNode json = parseResult(result);
        assertTrue("Response does not have formErrors", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());

        // 2 times because of the mock call
        verify(ssoForm, times(2)).hasErrors();
        verify(wsClient, never()).url(any(String.class));
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Testing a valid login");

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID);
        data.put("is_valid", true);
        ObjectNode tokenJsonResponse = Json.newObject();
        tokenJsonResponse.set("data",data);
        mockTokenRequest(tokenJsonResponse);

        ObjectNode emailJsonResponse = Json.newObject();
        emailJsonResponse.put("email", FAKE_USER_EMAIL);
        mockEmailRequest(emailJsonResponse);

        User user = new User();
        user.setId(1);
        ArgumentCaptor<Supplier> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        when(jpaApi.withTransaction(supplierArgumentCaptor.capture())).thenReturn(user);

        Result result = getResultFromController();
        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertFalse("Response has an error message", json.has("message"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertTrue("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have the url key", json.has("url"));
        assertEquals("Response does not have correct url value", controllers.secured.routes.HomeController.index().url(), json.get("url").asText());

        verify(jpaApi).withTransaction(supplierArgumentCaptor.capture());
    }

    @Test
    public void testLoginWithNoUser() {
        Logger.debug("Testing a login with no user account");

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID);
        data.put("is_valid", true);
        ObjectNode tokenJsonResponse = Json.newObject();
        tokenJsonResponse.set("data",data);
        mockTokenRequest(tokenJsonResponse);

        ObjectNode emailJsonResponse = Json.newObject();
        emailJsonResponse.put("email", FAKE_USER_EMAIL);
        mockEmailRequest(emailJsonResponse);

        ArgumentCaptor<Supplier> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        when(jpaApi.withTransaction(supplierArgumentCaptor.capture())).thenThrow(new EnfException());

        Result result = getResultFromController();
        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have an error message", json.has("message"));
        assertEquals("Response has an incorrect error message", "No account", json.get("message").asText());
        assertTrue("Response does not have the user email", json.has("email"));
        assertEquals("Response has an incorrect user email", FAKE_USER_EMAIL, json.get("email").asText());

        verify(jpaApi).withTransaction(supplierArgumentCaptor.capture());
    }

    @Test
    public void testLoginWithInvalidToken() {
        Logger.debug("Testing a login with an invalid Facebook Token");

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID + "INCORRECT");
        data.put("is_valid", false);
        ObjectNode tokenJsonResponse = Json.newObject();
        mockTokenRequest(tokenJsonResponse);
        ObjectNode emailJsonResponse = Json.newObject();
        emailJsonResponse.put("email", FAKE_USER_EMAIL);
        mockEmailRequest(emailJsonResponse);

        Result result = getResultFromController();
        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have an error message", json.has("message"));
        assertNotEquals("Response has an incorrect error message", "No account", json.get("message").asText());
        assertFalse("Response has the user email", json.has("email"));

        ArgumentCaptor<Supplier> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(jpaApi, never()).withTransaction(supplierArgumentCaptor.capture());
    }

    @Test
    public void testLoginWithInvalidAppIdInResponse() {
        Logger.debug("Testing a login with Invalid Facebook Response (1)");

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID + "INCORRECT");
        data.put("is_valid", true);
        ObjectNode tokenJsonResponse = Json.newObject();
        tokenJsonResponse.set("data",data);
        mockTokenRequest(tokenJsonResponse);

        ObjectNode emailJsonResponse = Json.newObject();
        emailJsonResponse.put("email", FAKE_USER_EMAIL);
        mockEmailRequest(emailJsonResponse);

        Result result = getResultFromController();
        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have an error message", json.has("message"));
        assertNotEquals("Response has an incorrect error message", "No account", json.get("message").asText());
        assertFalse("Response has the user email", json.has("email"));

        ArgumentCaptor<Supplier> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(jpaApi, never()).withTransaction(supplierArgumentCaptor.capture());
    }

    @Test
    public void testLoginWithInvalidEmailInResponse() {
        Logger.debug("Testing a login with Invalid Facebook Response (2)");

        ObjectNode data = Json.newObject();
        data.put("app_id", FAKE_APP_ID + "INCORRECT");
        data.put("is_valid", true);
        ObjectNode tokenJsonResponse = Json.newObject();
        tokenJsonResponse.set("data",data);
        mockTokenRequest(tokenJsonResponse);
        mockEmailRequest(Json.newObject());

        Result result = getResultFromController();
        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have an error message", json.has("message"));
        assertNotEquals("Response has an incorrect error message", "No account", json.get("message").asText());
        assertFalse("Response has the user email", json.has("email"));

        ArgumentCaptor<Supplier> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(jpaApi, never()).withTransaction(supplierArgumentCaptor.capture());
    }

    private void mockEmailRequest(ObjectNode json) {
        CompletableFuture<JsonNode> jsonPromise = CompletableFuture.completedFuture(json);
        ArgumentCaptor<Function> functionArgumentCaptor = ArgumentCaptor.forClass(Function.class);

        when(wsClient.url(startsWith(FB_USER_EMAIL_ENDPOINT))).thenReturn(wsEmailRequest);
        when(wsEmailRequest.setQueryParameter(eq("fields"), anyString())).thenReturn(wsEmailRequest);
        when(wsEmailRequest.setQueryParameter(eq("access_token"), anyString())).thenReturn(wsEmailRequest);
        when(wsEmailRequest.get()).thenReturn(unusedPromise2);
        when(unusedPromise2.thenApply(functionArgumentCaptor.capture())).thenReturn(wsEmailPromise);
        when(wsEmailPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(wsEmailPromise);
        when(wsEmailPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(jsonPromise);

    }

    private void mockTokenRequest(ObjectNode json) {
        CompletableFuture<JsonNode> jsonPromise = CompletableFuture.completedFuture(json);
        ArgumentCaptor<Function> functionArgumentCaptor = ArgumentCaptor.forClass(Function.class);

        when(wsClient.url(eq(FB_TOKEN_ENDPOINT))).thenReturn(wsTokenRequest);
        when(wsTokenRequest.setQueryParameter(eq("input_token"), anyString())).thenReturn(wsTokenRequest);
        when(wsTokenRequest.setQueryParameter(eq("access_token"), anyString())).thenReturn(wsTokenRequest);
        when(wsTokenRequest.get()).thenReturn(unusedPromise1);
        when(unusedPromise1.thenApply(functionArgumentCaptor.capture())).thenReturn(wsTokenPromise);
        when(wsTokenPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(wsTokenPromise);
        when(wsTokenPromise.thenApply(functionArgumentCaptor.capture())).thenReturn(jsonPromise);
    }

    private Result getResultFromController() {
        CompletionStage<Result> resultPromise = controller.login();
        try {
            Result result = resultPromise.toCompletableFuture().get();
            assertEquals("Status did not return OK", OK, result.status());
            assertTrue("Response was not json", result.contentType().toString().contains("application/json"));
            return result;
        } catch (Exception e) {
            fail("An unexepceted exception was thrown");
        }
        return null;
    }
}
