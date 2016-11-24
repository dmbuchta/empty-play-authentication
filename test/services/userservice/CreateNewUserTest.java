package services.userservice;

import controllers.security.SecurityController;
import models.User;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import play.Logger;
import services.exceptions.DuplicateEntityException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by Dan on 11/19/2016.
 */
public class CreateNewUserTest extends UserServiceTest {

    private SecurityController.NewUserForm newUserForm;

    @Override
    public void setUp() {
        super.setUp();
        newUserForm = new SecurityController.NewUserForm();
        newUserForm.setNewEmail("testemail@playframework.com");
        newUserForm.setNewPassword("password");
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testCreatingNewUser() {
        Logger.debug("Running New User Test");
        try {
            doNothing().when(mockedRepo).save(any(User.class), eq(true));
            User user = userService.createNewUser(newUserForm);
            assertTrue("Expected the user's email to match", user.getEmail().equalsIgnoreCase(newUserForm.getNewEmail()));
            assertTrue("Expected the user's password to match", user.getPassword().equals(newUserForm.getNewPassword()));
            assertTrue("Expected the user's date to not be null", user.getCreationDate() != null);
            verify(mockedRepo).save(any(User.class), eq(true));
        } catch (PSQLException e) {
            fail("Somehow, a PSQLException was thrown...");
        }
    }

    @Test
    public void testDuplicateUserCreation() {
        Logger.debug("Running Duplicate New User Test");
        Exception exception = mock(PSQLException.class);
        when(exception.getMessage()).thenReturn("violates unique constraint");
        try {
            doThrow(exception).when(mockedRepo).save(any(User.class), eq(true));
            userService.createNewUser(newUserForm);
        } catch (DuplicateEntityException e) {
            try {
                verify(mockedRepo).save(any(User.class), eq(true));
            } catch (PSQLException e1) {
                fail("Somehow, a PSQLException was thrown (1)...");
            }
            return;
        } catch (PSQLException e) {
            fail("Somehow, a PSQLException was thrown (2)...");
        }
        fail("Expected a Duplicate Entity Exception to be thrown");
    }

}
