package controllers.secure.securitycontroller;

import controllers.secure.SecurityController;
import models.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.Application;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import play.test.WithApplication;
import services.UserService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

/**
 * Created by Dan on 11/19/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginApplicationTest extends WithApplication {

    @Mock
    FormFactory formFactory;
    @Mock
    UserService userService;
    @Mock
    protected Form<User> userForm;
    @Mock
    protected Form<SecurityController.NewUserForm> newUserForm;

    @Override
    protected Application provideApplication() {

        return new GuiceApplicationBuilder()
                .configure("db.default.driver", "org.h2.Driver")
                .configure("db.default.url", "jdbc:h2:mem:play;MODE=PostgreSQL")
                .configure("jpa.default", "testPersistenceUnit")
                .build();
    }

    @Test
    public void testShowLoginPage() {
        Logger.debug("Testing the login page" );

        Result result = route(fakeRequest("GET", "/login"));

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not text/html", result.contentType().toString().contains("text/html"));

        String html = contentAsString(result);
        assertTrue("Login page does not have the Log in Form", html.contains("Log in to your account"));
        assertTrue("Login page does not have create account modal", html.contains("Create Account"));
        assertFalse("Login page is showing an error message", html.contains("There was a problem with your login"));
    }

    @Test
    public void testLoginSuccessfully() {
        Logger.debug("Testing a successful login" );

        Http.RequestBuilder requestBuilder = fakeRequest("POST", "/login");
        Map<String, String> data = new HashMap<>();
        data.put("email", "testemail@playframework.com");
        data.put("password", "passwd");
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        assertEquals("Status is not a redirect", SEE_OTHER, result.status());
        assertEquals("Login did not redirect to home page", "/", result.redirectLocation().get());
    }

    @Test
    public void testLoginUnsuccessfully() {
        Logger.debug("Testing an unsuccessful login" );

        Http.RequestBuilder requestBuilder = fakeRequest("POST", "/login");
        Map<String, String> data = new HashMap<>();
        data.put("email", "testemail@playframework.com");
        data.put("password", "password");
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        assertEquals("Status is not OK", OK, result.status());
        assertTrue("Result is not text/html", result.contentType().toString().contains("text/html"));

        String html = contentAsString(result);
        assertTrue("Login page does not have the Log in Form", html.contains("Log in to your account"));
        assertTrue("Login page does not have create account modal", html.contains("Create Account"));
        assertTrue("Login page is showing an error message", html.contains("There was a problem with your login"));
    }

    @Test
    public void testServerSideValidation() {
        Logger.debug("Testing server side validation" );

        Http.RequestBuilder requestBuilder = fakeRequest("POST", "/login");
        Map<String, String> data = new HashMap<>();
        requestBuilder.bodyForm(data);
        Result result = route(requestBuilder);

        String html = contentAsString(result);
        assertTrue("Login page does not show required validation errors", html.contains("This field is required"));

        requestBuilder = fakeRequest("POST", "/login");
        data.put("email", "invalidEmail");
        requestBuilder.bodyForm(data);
        result = route(requestBuilder);
        html = contentAsString(result);
        assertTrue("Login page does not show email validation errors", html.contains("Valid email required"));
    }

}
