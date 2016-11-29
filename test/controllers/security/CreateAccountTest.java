package controllers.security;

import com.fasterxml.jackson.databind.JsonNode;
import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.postgresql.util.PSQLException;
import play.Configuration;
import play.Logger;
import play.data.Form;
import play.mvc.Result;
import services.AccountService;
import services.exceptions.DuplicateEntityException;
import utils.TestUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static utils.TestConstants.FAKE_USER_ID;

/**
 * Created by Dan on 11/20/2016.
 */
public class CreateAccountTest extends LoginControllerTest {
    @Mock
    private AccountService.NewUserForm newUserInput;
    @Mock
    protected Form<AccountService.NewUserForm> newUserForm;
    @Mock
    private AccountService accountService;
    @Mock
    private Configuration configuration;

    @InjectMocks
    private SimpleLoginController controller;

    @Override
    public void setUp() {
        super.setUp();
        when(formFactory.form(AccountService.NewUserForm.class)).thenReturn(newUserForm);
        when(newUserForm.bindFromRequest()).thenReturn(newUserForm);
        when(newUserForm.get()).thenReturn(newUserInput);
    }

    @Test
    public void testAccountCreationWithErrors() {
        Logger.debug("Testing creating an account with form errors");
        when(newUserForm.hasErrors()).thenReturn(true);

        Result result = getResultFromController(controller);
        JsonNode json = TestUtils.parseResult(result);
        assertTrue("Response does not have formErrors", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());

        try {
            verify(newUserForm, times(2)).hasErrors();
            verify(accountService, never()).createNewAccount(newUserInput);
        } catch (PSQLException e) {
            fail("This should never happen (2)");
        }
    }

    @Test
    public void testExistingAccountCreation() {
        Logger.debug("Testing creating an account that already exists");
        when(newUserForm.hasErrors()).thenReturn(false);
        try {
            when(accountService.createNewAccount(newUserInput)).thenThrow(new DuplicateEntityException());
        } catch (PSQLException e) {
            fail("This should never happen (1)");
        }

        Result result = getResultFromController(controller);

        JsonNode json = TestUtils.parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertTrue("Response does not have an error message", json.has("message"));
        assertEquals("The Response error message is incorrect", "There is already an account with this email address.", json.get("message").asText());
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());

        try {
            verify(accountService).createNewAccount(newUserInput);
        } catch (PSQLException e) {
            fail("This should never happen (2)");
        }
    }

    @Test
    public void testValidAccountCreation() {
        Logger.debug("Testing creating an account");
        User user = new User();
        user.setId(FAKE_USER_ID);

        when(newUserForm.hasErrors()).thenReturn(false);
        when(newUserForm.get()).thenReturn(newUserInput);
        when(session.get(eq("uId"))).thenReturn(FAKE_USER_ID + "");
        try {
            when(accountService.createNewAccount(newUserInput)).thenReturn(user);
        } catch (PSQLException e) {
            fail("This should never happen (1)");
        }

        Result result = getResultFromController(controller);
        JsonNode json = TestUtils.parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertFalse("Response has an error message", json.has("message"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertTrue("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have the url key", json.has("url"));
        assertEquals("Response does not have correct url value", controllers.secured.routes.HomeController.index().url(), json.get("url").asText());
        assertTrue("User is not being stored on session", Authenticator.isUserLoggedIn(context));

        try {
            verify(accountService).createNewAccount(newUserInput);
        } catch (PSQLException e) {
            fail("This should never happen (2)");
        }
    }

    @Override
    protected Result getResultFromController(LoginController controller) {
        Result result = ((SimpleLoginController) controller).createAccount();
        assertTrue("Result type is not json", result.contentType().toString().contains("application/json"));
        assertEquals("Result status is not OK", result.status(), OK);
        return result;
    }
}
