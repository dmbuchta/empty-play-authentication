package controllers.secure.securitycontroller;

import controllers.secure.Authenticator;
import controllers.secure.routes;
import models.User;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import play.Logger;
import play.data.Form;
import play.mvc.Result;
import services.exceptions.EnfException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.SEE_OTHER;

/**
 * Created by Dan on 11/19/2016.
 */
public class LoginTest extends SecurityControllerTest {

    private User user;
    @Mock
    protected Form<User> loginForm;

    @Override
    public void setUp() {
        super.setUp();
        when(session.get(any(String.class))).thenReturn("1");
        when(formFactory.form(User.class)).thenReturn(loginForm);
        when(loginForm.bindFromRequest()).thenReturn(loginForm);
    }

    @Test
    public void testLoginWithErrors() {
        Logger.debug("Testing login with form errors");
        when(loginForm.hasErrors()).thenReturn(true);
        try {
            Result result = controller.login();
        } catch (NullPointerException npe) {
            assertTrue("Looks like a npe occurred for the wrong reasonse", npe.getStackTrace()[0].toString().contains("template.scala"));
            Logger.warn("Template cannot be rendered because the form is mocked", npe);
        }
        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(userService, never()).login(argument.capture());
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Testing a valid login");
        user = new User();
        user.setId(100000);

        when(loginForm.get()).thenReturn(user);
        when(userService.login(user)).thenReturn(user);
        Result result = controller.login();

        assertEquals("Did not redirect after logging in", result.status(), SEE_OTHER);
        assertEquals("Did not redirect to home page", routes.HomeController.index().url(), result.redirectLocation().get());
        assertTrue("User is not being stored on session", Authenticator.isUserLoggedIn(context));
        verify(userService).login(user);
    }

    @Test
    public void testLoginWithInvalidCredentials() {
        Logger.debug("Testing login with invalid credentials");
        when(loginForm.get()).thenReturn(user);
        when(userService.login(user)).thenThrow(new EnfException());

        try {
            Result result = controller.login();
        } catch (NullPointerException npe) {
            assertTrue("Looks like a npe occurred for the wrong reasonse", npe.getStackTrace()[0].toString().contains("template.scala"));
            Logger.warn("Template cannot be rendered because the form is mocked", npe);
        }
        verify(userService).login(user);
    }





    /* This does not work as desired because I cannot figure out how to create the Formatter
    public FormFactory getFormFactory() {
        // SEE: http://bval.apache.org/obtaining-a-validator.html
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validatorFactory.getValidator();

        Environment environment = Environment.simple(new File("application.conf"), Mode.Test());
        MessagesApi messagesApi = new MessagesApi(new DefaultMessagesApi(environment, Configuration.load(environment), new DefaultLangs(Configuration.load(environment))));

        return new FormFactory(messagesApi, null, validatorFactory.getValidator());
    }
    */
}
