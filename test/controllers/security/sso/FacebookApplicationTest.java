package controllers.security.sso;

import org.junit.Test;
import play.Logger;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import utils.ApplicationTest;
import utils.Configs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

/**
 * Created by Dan on 11/27/2016.
 */
public class FacebookApplicationTest extends ApplicationTest {

    private static final String FAKE_APP_ID = "FAKE_APP_ID";
    private static final String FAKE_APP_SECRET = "FAKE_APP_SECRET";

    @Override
    public GuiceApplicationBuilder configureApp(GuiceApplicationBuilder builder) {
        return super.configureApp(builder)
                .configure(Configs.FB_APP_ID, FAKE_APP_ID)
                .configure(Configs.FB_APP_SECRET, FAKE_APP_SECRET);
    }

    @Test
    public void testShowLoginPage() {
        Logger.debug("Testing the login page with Facebook Sign in configured");

        Result result = route(fakeRequest("GET", "/login"));

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not text/html", result.contentType().toString().contains("text/html"));

        String html = contentAsString(result);
        assertFalse("Login page has incorrect sign in header", html.contains("Sign in to your account"));
        assertTrue("Login page does not have the Facebook Sign in button", html.contains("Sign in with Facebook"));
        assertFalse("Login page has the Google Sign in button", html.contains("Sign in with Google"));
        assertTrue("Login page does not have create account modal", html.contains("Create Account"));
        assertFalse("Login page is showing an error message", html.contains("There was a problem with your login"));
    }
}
