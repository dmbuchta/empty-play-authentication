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
import services.SessionCache;
import services.UserService;
import services.exceptions.DuplicateEntityException;
import utils.TestUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static utils.TestConstants.FAKE_SESSION_ID;
import static utils.TestConstants.FAKE_USER_ID;
import static utils.TestConstants.SESSION_ID_PARAM;

/**
 * Created by Dan on 11/20/2016.
 */
public class CreateAccountTest extends LoginControllerTest {
    @Mock
    private UserService.NewUserForm newUserInput;
    @Mock
    protected Form<UserService.NewUserForm> newUserForm;
    @Mock
    private UserService userService;
    @Mock
    private Configuration configuration;
    @InjectMocks
    private SimpleLoginController controller;

    @Override
    public void setUp() {
        super.setUp();
        when(formFactory.form(UserService.NewUserForm.class)).thenReturn(newUserForm);
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
            verify(userService, never()).createNewAccount(newUserInput);
            verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));
        } catch (PSQLException e) {
            fail("This should never happen (2)");
        }
    }

    @Test
    public void testExistingAccountCreation() {
        Logger.debug("Testing creating an account that already exists");
        when(newUserForm.hasErrors()).thenReturn(false);
        try {
            when(userService.createNewAccount(newUserInput)).thenThrow(new DuplicateEntityException());
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
            verify(userService).createNewAccount(newUserInput);
            verify(sessionCache, never()).addUserToCache(anyString(), any(User.class));

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
        try {
            when(userService.createNewAccount(newUserInput)).thenReturn(user);
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
        assertEquals("Response does not have correct url value", controllers.secured.html.routes.UserController.index().url(), json.get("url").asText());

        try {
            verify(userService).createNewAccount(newUserInput);
            verify(sessionCache).addUserToCache(anyString(), eq(user));
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
