package controllers.security;

import org.junit.Test;
import play.Logger;
import play.mvc.Http;
import play.mvc.Result;
import utils.ApplicationTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.*;

/**
 * Created by Dan on 11/19/2016.
 */
public class LoginApplicationTest extends ApplicationTest {

    @Test
    public void testShowLoginPage() {
        Logger.debug("Testing the login page");

        Result result = route(fakeRequest("GET", controllers.security.routes.SecurityController.showLoginPage().url()));

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not text/html", result.contentType().toString().contains("text/html"));

        String html = contentAsString(result);
        assertTrue("Login page does not have the correct sign in header", html.contains("Sign in to your account"));
        assertFalse("Login page has the Facebook Sign in button", html.contains("Sign in with Facebook"));
        assertFalse("Login page has the Google Sign in button", html.contains("Sign in with Google"));
        assertTrue("Login page does not have create account modal", html.contains("Create Account"));
        assertFalse("Login page is showing an error message", html.contains("There was a problem with your login"));
    }

    @Test
    public void testLoginSuccessfully() {
        Logger.debug("Testing a successful login");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.routes.SecurityController.login().url());
        Map<String, String> data = new HashMap<>();
        data.put("email", "testemail@playframework.com");
        data.put("password", "passwd");
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        assertEquals("Status is not a redirect", SEE_OTHER, result.status());
        assertEquals("Login did not redirect to home page", controllers.secured.routes.HomeController.index().url(), result.redirectLocation().get());
    }

    @Test
    public void testEmailIsCaseInsensitive() {
        Logger.debug("Testing a successful login");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.routes.SecurityController.login().url());
        Map<String, String> data = new HashMap<>();
        data.put("email", "testEMAIL@playframework.com");
        data.put("password", "passwd");
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        assertEquals("Status is not a redirect", SEE_OTHER, result.status());
        assertEquals("Login did not redirect to home page", controllers.secured.routes.HomeController.index().url(), result.redirectLocation().get());
    }

    @Test
    public void testLoginUnsuccessfully() {
        Logger.debug("Testing an unsuccessful login");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.routes.SecurityController.login().url());
        Map<String, String> data = new HashMap<>();
        data.put("email", "testemail@playframework.com");
        data.put("password", "password");
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not text/html", result.contentType().toString().contains("text/html"));

        String html = contentAsString(result);
        assertTrue("Login page does not have the Log in Form", html.contains("Sign in to your account"));
        assertTrue("Login page does not have create account modal", html.contains("Create Account"));
        assertTrue("Login page is not showing an error message", html.contains("There was a problem with your login"));
    }

    @Test
    public void testServerSideValidation() {
        Logger.debug("Testing server side validation");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.routes.SecurityController.login().url());
        Map<String, String> data = new HashMap<>();
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        String html = contentAsString(result);
        assertTrue("Login page does not show required validation errors", html.contains("This field is required"));

        requestBuilder = fakeRequest("POST", controllers.security.routes.SecurityController.login().url());
        data.put("email", "invalidEmail");
        requestBuilder.bodyForm(data);
        result = route(requestBuilder);
        html = contentAsString(result);
        assertTrue("Login page does not show email validation errors", html.contains("Valid email required"));
    }

    @Test
    public void testUnconfiguredGoogleLogin() {
        Logger.debug("Testing an un-configured Google login");

        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.sso.routes.GoogleSsoController.login().url());
        Result result = route(requestBuilder);

        assertEquals("Status is not a 501", NOT_IMPLEMENTED, result.status());
    }

    @Test
    public void testUnconfiguredFacebookLogin() {
        Logger.debug("Testing an un-configured Facebook login");
        Http.RequestBuilder requestBuilder = fakeRequest("POST", controllers.security.sso.routes.FacebookSsoController.login().url());
        Result result = route(requestBuilder);
        assertEquals("Status is not a 501", NOT_IMPLEMENTED, result.status());
    }

}
