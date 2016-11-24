package controllers.secure.securitycontroller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.secure.SecurityController;
import controllers.secure.routes;
import models.User;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.postgresql.util.PSQLException;
import play.Logger;
import play.data.Form;
import play.mvc.Result;
import services.exceptions.DuplicateEntityException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static utils.TestUtils.parseResult;

/**
 * Created by Dan on 11/20/2016.
 */
public class CreateAccountTest extends SecurityControllerTest {

    @Mock
    private SecurityController.NewUserForm newUserInput;
    @Mock
    protected Form<SecurityController.NewUserForm> newUserForm;

    @Override
    public void setUp() {
        super.setUp();
        when(formFactory.form(SecurityController.NewUserForm.class)).thenReturn(newUserForm);
        when(newUserForm.bindFromRequest()).thenReturn(newUserForm);
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testAccountCreationWithErrors() {
        Logger.debug("Testing creating an account with form errors");
        when(newUserForm.hasErrors()).thenReturn(true);
        Result result = controller.createAccount();

        assertEquals("Status did not return OK", OK, result.status());
        assertTrue("Response was not json", result.contentType().toString().contains("application/json"));

        ObjectNode json = parseResult(result);
        assertTrue("Response does not have formErrors", json.has("formErrors"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());

        ArgumentCaptor<SecurityController.NewUserForm> argument = ArgumentCaptor.forClass(SecurityController.NewUserForm.class);
        try {
            verify(newUserForm, times(2)).hasErrors();
            verify(userService, never()).createNewUser(argument.capture());
        } catch (PSQLException e) {
            fail("This should never happen (2)");
        }
    }

    @Test
    public void testExistingAccountCreation() {
        Logger.debug("Testing creating an account that already exists");
        when(newUserForm.hasErrors()).thenReturn(false);
        when(newUserForm.get()).thenReturn(newUserInput);
        try {
            when(userService.createNewUser(newUserInput)).thenThrow(new DuplicateEntityException());
        } catch (PSQLException e) {
            fail("This should never happen (1)");
        }

        Result result = controller.createAccount();
        assertEquals("Status did not return OK", OK, result.status());
        assertTrue("Response was not json", result.contentType().toString().contains("application/json"));

        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertTrue("Response does not have an error message", json.has("message"));
        assertEquals("The Response error message is incorrect", "There is already an account with this email address.", json.get("message").asText());
        assertTrue("Response does not have the success key", json.has("success"));
        assertFalse("Response does not have correct success value", json.get("success").asBoolean());

        try {
            verify(userService).createNewUser(newUserInput);
        } catch (PSQLException e) {
            fail("This should never happen (2)");
        }
    }

    @Test
    public void testValidAccountCreation() {
        Logger.debug("Testing creating an account");
        User user = new User();
        user.setId(10000);

        when(newUserForm.hasErrors()).thenReturn(false);
        when(newUserForm.get()).thenReturn(newUserInput);
        try {
            when(userService.createNewUser(newUserInput)).thenReturn(user);
        } catch (PSQLException e) {
            fail("This should never happen (1)");
        }

        Result result = controller.createAccount();
        assertEquals("Status did not return OK", OK, result.status());
        assertTrue("Response was not json", result.contentType().toString().contains("application/json"));

        ObjectNode json = parseResult(result);
        assertFalse("Response has formErrors when it shouldn't", json.has("formErrors"));
        assertFalse("Response has an error message", json.has("message"));
        assertTrue("Response does not have the success key", json.has("success"));
        assertTrue("Response does not have correct success value", json.get("success").asBoolean());
        assertTrue("Response does not have the url key", json.has("url"));
        assertEquals("Response does not have correct url value", routes.HomeController.index().url(), json.get("url").asText());

        try {
            verify(userService).createNewUser(newUserInput);
        } catch (PSQLException e) {
            fail("This should never happen (2)");
        }
    }
}
