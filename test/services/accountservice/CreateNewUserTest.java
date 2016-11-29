package services.accountservice;

import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.postgresql.util.PSQLException;
import play.Logger;
import repositories.UserRepository;
import services.AccountService;
import services.exceptions.DuplicateEntityException;
import utils.UnitTest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by Dan on 11/19/2016.
 */
public class CreateNewUserTest extends UnitTest {

    private AccountService.NewUserForm newUserForm;
    @Mock
    private UserRepository repository;
    @InjectMocks
    private AccountService accountService;

    @Override
    public void setUp() {
        super.setUp();
        newUserForm = new AccountService.NewUserForm();
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
            doNothing().when(repository).save(any(User.class), eq(true));
            User user = accountService.createNewAccount(newUserForm);
            assertTrue("Expected the user's email to match", user.getEmail().equalsIgnoreCase(newUserForm.getNewEmail()));
            assertTrue("Expected the user's password to match", user.getPassword().equals(newUserForm.getNewPassword()));
            assertTrue("Expected the user's date to not be null", user.getCreationDate() != null);
            verify(repository).save(any(User.class), eq(true));
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
            doThrow(exception).when(repository).save(any(User.class), eq(true));
            accountService.createNewAccount(newUserForm);
        } catch (DuplicateEntityException e) {
            try {
                verify(repository).save(any(User.class), eq(true));
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
