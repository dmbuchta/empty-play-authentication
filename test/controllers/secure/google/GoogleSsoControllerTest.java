package controllers.secure.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.secure.GoogleSsoController;
import models.User;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import play.Configuration;
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
 * Created by Dan on 11/23/2016.
 */
public class GoogleSsoControllerTest extends UnitTest {

    private static final String FAKE_CLIENT_ID = "THIS_IS_A_FAKE_GOOGLE_CLIENT_ID";
    private GoogleSsoController controller;

    @Mock
    private FormFactory formFactory;
    @Mock
    private JPAApi jpaApi;
    @Mock
    private UserService userService;
    @Mock
    private WSClient wsClient;
    @Mock
    private Configuration configuration;
    @Mock
    private Form<GoogleSsoController.GoogleSsoForm> ssoForm;
    @Mock
    private GoogleSsoController.GoogleSsoForm ssoInfo;
    @Mock
    private Http.Context context;
    @Mock
    private Http.Session session;
    @Mock
    private WSRequest wsRequest;
    @Mock
    private CompletionStage<WSResponse> wsResponsePromise;

    @Override
    public void setUp() {
        super.setUp();
        Http.Context.current.set(context);
        when(context.session()).thenReturn(session);
        when(formFactory.form(GoogleSsoController.GoogleSsoForm.class)).thenReturn(ssoForm);
        when(ssoForm.bindFromRequest()).thenReturn(ssoForm);
        when(configuration.getString(eq("sso.client.id"))).thenReturn(FAKE_CLIENT_ID);
        controller = new GoogleSsoController(jpaApi, formFactory, userService, wsClient, configuration);
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

        ObjectNode jsonResponse = Json.newObject();
        jsonResponse.put("aud", FAKE_CLIENT_ID);
        jsonResponse.put("email", "testemail@playframework.com");
        setUpWSMocking(jsonResponse);

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
        assertEquals("Response does not have correct url value", controllers.secure.routes.HomeController.index().url(), json.get("url").asText());

        verify(jpaApi).withTransaction(supplierArgumentCaptor.capture());
    }


    @Test
    public void testLoginWithNoUser() {
        Logger.debug("Testing a login with no user account");

        ObjectNode jsonResponse = Json.newObject();
        jsonResponse.put("aud", FAKE_CLIENT_ID);
        jsonResponse.put("email", "testemail@playframework.com");
        setUpWSMocking(jsonResponse);

        ArgumentCaptor<Supplier> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        when(jpaApi.withTransaction(supplierArgumentCaptor.capture())).thenThrow(new EnfException());

        Result result = getResultFromController();
        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response has an error message", json.has("message"));
        assertEquals("Response has an error message", "No account", json.get("message").asText());

        verify(jpaApi).withTransaction(supplierArgumentCaptor.capture());
    }

    @Test
    public void testLoginWithInvalidGoogleResponse() {
        Logger.debug("Testing a login with Invalid Google Response");

        ObjectNode jsonResponse = Json.newObject();
        jsonResponse.put("aud", FAKE_CLIENT_ID + "_SOME_ADDITIONAL_VALUE");
        setUpWSMocking(jsonResponse);

        Result result = getResultFromController();
        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response has an error message", json.has("message"));
        assertEquals("Response has an error message", "There was a problem with your login", json.get("message").asText());

        ArgumentCaptor<Supplier> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(jpaApi, never()).withTransaction(supplierArgumentCaptor.capture());
    }

    private void setUpWSMocking(ObjectNode json) {
        CompletableFuture<JsonNode> jsonPromise = CompletableFuture.completedFuture(json);
        ArgumentCaptor<Function> functionArgumentCaptor = ArgumentCaptor.forClass(Function.class);

        when(ssoForm.get()).thenReturn(ssoInfo);
        when(ssoInfo.getId_token()).thenReturn("");
        when(wsClient.url(anyString())).thenReturn(wsRequest);
        when(wsRequest.setQueryParameter(anyString(), anyString())).thenReturn(wsRequest);
        when(wsRequest.post(anyString())).thenReturn(wsResponsePromise);
        when(wsResponsePromise.thenApply(functionArgumentCaptor.capture())).thenReturn(wsResponsePromise);
        when(wsResponsePromise.thenApply(functionArgumentCaptor.capture())).thenReturn(jsonPromise);
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
