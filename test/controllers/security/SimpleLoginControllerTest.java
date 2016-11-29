package controllers.security;

import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.Configuration;
import play.Logger;
import play.data.Form;
import play.mvc.Result;
import services.AccountService;
import services.exceptions.EnfException;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.SEE_OTHER;
import static utils.TestConstants.FAKE_USER_ID;

/**
 * Created by Dan on 11/28/2016.
 */
public class SimpleLoginControllerTest extends LoginControllerTest {

    @Mock
    private Form<User> loginForm;
    @Mock
    private AccountService accountService;
    @Mock
    private Configuration configuration;

    @InjectMocks
    private SimpleLoginController controller;

    @Override
    public void setUp() {
        super.setUp();
        when(formFactory.form(User.class)).thenReturn(loginForm);
        when(loginForm.bindFromRequest()).thenReturn(loginForm);
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Testing a valid login");
        User user = new User();
        user.setId(FAKE_USER_ID);
        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenReturn(CompletableFuture.completedFuture(user));
        when(session.get(eq("uId"))).thenReturn(FAKE_USER_ID + "");

        Result result = getResultFromController(controller);
        assertEquals("Did not redirect after logging in", result.status(), SEE_OTHER);
        assertEquals("Did not redirect to home page", controllers.secured.routes.HomeController.index().url(), result.redirectLocation().get());
        assertTrue("User is not being stored on session", Authenticator.isUserLoggedIn(context));
        verify(loginService).login(loginForm);
    }

    @Test
    public void testBadLogin() {
        Logger.debug("Testing login with invalid credentials");
        when(loginForm.hasErrors()).thenReturn(false);
        when(loginService.login(loginForm)).thenThrow(new EnfException());

        try {
            getResultFromController(controller);
        } catch (NullPointerException npe) {
            assertTrue("Looks like a npe occurred for the wrong reasons", npe.getStackTrace()[0].toString().contains("template.scala"));
            Logger.warn("Template cannot be rendered because the form is mocked", npe);
        }
        verify(loginService).login(loginForm);
    }

    @Test
    public void testLoginWithFormErrors() {
        Logger.debug("Testing login with form errors");
        when(loginForm.hasErrors()).thenReturn(true);
        try {
            getResultFromController(controller);
        } catch (NullPointerException npe) {
            assertTrue("Looks like a npe occurred for the wrong reasons", npe.getStackTrace()[0].toString().contains("template.scala"));
            Logger.warn("Template cannot be rendered because the form is mocked", npe);
        }
        verify(loginService, never()).login(loginForm);
    }
}
