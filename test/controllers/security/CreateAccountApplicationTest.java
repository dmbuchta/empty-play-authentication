package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import play.Logger;
import play.mvc.Http;
import play.mvc.Result;
import utils.ApplicationTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static utils.TestConstants.*;
import static utils.TestUtils.parseResult;

/**
 * Created by Dan on 11/23/2016.
 */
public class CreateAccountApplicationTest extends ApplicationTest {

    @Test
    public void testCreateAccountSuccessfully() {
        Logger.debug("Testing a successful account creation");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.routes.SimpleLoginController.createAccount().url());
        Map<String, String> data = new HashMap<>();
        data.put("newEmail", FAKE_EMAIL_2);
        data.put("newPassword", FAKE_PASS);
        data.put("confirmPassword", FAKE_PASS);
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not application/json", result.contentType().toString().contains("application/json"));

        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors key", json.has("formErrors"));
        assertTrue("Response does not have success key", json.has("success"));
        assertTrue("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have url key", json.has("url"));
        assertEquals("Response does not have correct url value", controllers.secured.routes.HomeController.index().url(), json.get("url").asText());
    }

    @Test
    public void testDuplicateUserCreation() {
        Logger.debug("Testing a duplicate account creation");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.routes.SimpleLoginController.createAccount().url());
        Map<String, String> data = new HashMap<>();
        data.put("newEmail", FAKE_EMAIL);
        data.put("newPassword", FAKE_PASS);
        data.put("confirmPassword", FAKE_PASS);
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not application/json", result.contentType().toString().contains("application/json"));

        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors key", json.has("formErrors"));
        assertTrue("Response does not have success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have message key", json.has("message"));
        assertEquals("Response does not have correct url value", "There is already an account with this email address.", json.get("message").asText());
    }

    @Test
    public void testServerSideValidation() {
        Logger.debug("Testing server side validation");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.routes.SimpleLoginController.createAccount().url());
        Map<String, String> data = new HashMap<>();
        data.put("newEmail", INVALID_EMAIL);
        data.put("newPassword", INVALID_PASS);
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not application/json", result.contentType().toString().contains("application/json"));

        ObjectNode json = parseResult(result);
        assertTrue("Response does not have formErrors key", json.has("formErrors"));
        assertTrue("Response does not have success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());

        assertTrue("Response does not have correct email validation errors", json.get("formErrors").toString().contains("Valid email required"));
        assertTrue("Response does not have correct password validation errors", json.get("formErrors").toString().contains("Minimum length is"));
        assertTrue("Response does not have correct required validation errors", json.get("formErrors").toString().contains("This field is required"));
    }


}
