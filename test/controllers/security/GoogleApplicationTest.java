package controllers.security;

import org.junit.Test;
import play.Logger;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import utils.ApplicationTest;
import utils.Configs;

import static org.junit.Assert.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;
import static utils.TestConstants.FAKE_CLIENT_ID;

/**
 * Created by Dan on 11/27/2016.
 */
public class GoogleApplicationTest extends ApplicationTest {

    @Override
    public GuiceApplicationBuilder configureApp(GuiceApplicationBuilder builder) {
        return super.configureApp(builder)
                .configure(Configs.GOOGLE_CLIENT_ID, FAKE_CLIENT_ID);
    }

    @Test
    public void testShowLoginPage() {
        Logger.debug("Testing the login page with Google Sign in configured");

        Result result = route(fakeRequest("GET", controllers.security.routes.SimpleLoginController.showLoginPage().url()));

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not text/html", result.contentType().toString().contains("text/html"));

        String html = contentAsString(result);
        assertFalse("Login page has incorrect sign in header", html.contains("Sign in to your account"));
        assertFalse("Login page has the Facebook Sign in button", html.contains("id=\"fb-login-button\""));
        assertTrue("Login page does not have the Google Sign in button", html.contains("id=\"google-login-button\""));
        assertTrue("Login page does not have create account modal", html.contains("Create Account"));
        assertFalse("Login page is showing an error message", html.contains("There was a problem with your login"));
    }
}
