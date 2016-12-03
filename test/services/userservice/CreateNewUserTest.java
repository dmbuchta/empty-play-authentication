package services.userservice;

import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.postgresql.util.PSQLException;
import play.Logger;
import repositories.UserRepository;
import services.UserService;
import services.exceptions.DuplicateEntityException;
import utils.UnitTest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static utils.TestConstants.FAKE_EMAIL;
import static utils.TestConstants.FAKE_PASS;

/**
 * Created by Dan on 11/19/2016.
 */
public class CreateNewUserTest extends UnitTest {

    private UserService.NewUserForm newUserForm;
    @Mock
    private UserRepository repository;
    @InjectMocks
    private UserService userService;

    @Override
    public void setUp() {
        super.setUp();
        newUserForm = new UserService.NewUserForm();
        newUserForm.setNewEmail(FAKE_EMAIL);
        newUserForm.setNewPassword(FAKE_PASS);
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testCreatingNewUser() {
        Logger.debug("Running New User Test");
        try {
            doNothing().when(repository).saveAndFlush(any(User.class));
            User user = userService.createNewAccount(newUserForm);
            assertTrue("Expected the user's email to match", user.getEmail().equalsIgnoreCase(newUserForm.getNewEmail()));
            assertTrue("Expected the user's password to match", user.getPassword().equals(newUserForm.getNewPassword()));
            assertTrue("Expected the user's date to not be null", user.getCreationDate() != null);
            verify(repository).saveAndFlush(any(User.class));
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
            doThrow(exception).when(repository).saveAndFlush(any(User.class));
            userService.createNewAccount(newUserForm);
        } catch (DuplicateEntityException e) {
            try {
                verify(repository).saveAndFlush(any(User.class));
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
