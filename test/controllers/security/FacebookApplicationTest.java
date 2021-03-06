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
import static utils.TestConstants.FAKE_APP_ID;
import static utils.TestConstants.FAKE_APP_SECRET;
import static utils.TestUtils.fakeRequest;

/**
 * Created by Dan on 11/27/2016.
 */
public class FacebookApplicationTest extends ApplicationTest {

    @Override
    public GuiceApplicationBuilder configureApp(GuiceApplicationBuilder builder) {
        return super.configureApp(builder)
                .configure(Configs.FB_APP_ID, FAKE_APP_ID)
                .configure(Configs.FB_APP_SECRET, FAKE_APP_SECRET);
    }

    @Test
    public void testShowLoginPage() {
        Logger.debug("Testing the login page with Facebook Sign in configured");

        Result result = route(fakeRequest("GET", controllers.security.routes.SimpleLoginController.showLoginPage().url()));

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not text/html", result.contentType().toString().contains("text/html"));

        String html = contentAsString(result);
        assertFalse("Login page has incorrect sign in header", html.contains("Sign in to your account"));
        assertTrue("Login page does not have the Facebook Sign in button", html.contains("id=\"fb-login-button\""));
        assertFalse("Login page has the Google Sign in button", html.contains("id=\"google-login-button\""));
        assertTrue("Login page does not have create account modal", html.contains("Create Account"));
        assertFalse("Login page is showing an error message", html.contains("There was a problem with your login"));
    }
}
