package services.login;

import models.User;
import org.junit.Test;
import org.mockito.Mock;
import play.Logger;
import play.data.Form;
import services.exceptions.EnfException;
import services.login.impl.SimpleLoginService;
import utils.UnitTest;

import javax.persistence.NoResultException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Dan on 11/28/2016.
 */
public class SimpleLoginServiceTest extends LoginServiceTest {

    @Mock
    Form<User> userForm;

    @Override
    public void setUp() {
        super.setUp();
        service = new SimpleLoginService(repository);
        when(userForm.get()).thenReturn(user);
    }

    @Test
    public void testGoodLogin() {
        Logger.debug("Running Good Login Test");
        when(repository.findByEmailAndPassword(eq(user.getEmail()), not(eq(user.getPassword())))).thenReturn(user);

        User userResult = getUserFromPromise(service.login(userForm));
        assertTrue("The user was expected to be returned", userResult.equals(user));
        verify(repository).findByEmailAndPassword(eq(user.getEmail()), any(String.class));
    }

    @Test
    public void testBadLogin() {
        Logger.debug("Running Bad Login Test");
        when(repository.findByEmailAndPassword(eq(user.getEmail()), not(eq(user.getPassword())))).thenThrow(new NoResultException());

        try {
            service.login(userForm);
        } catch (Exception e) {
            assertTrue("An EnfException was expected to be thrown (1)", e instanceof EnfException);
            verify(repository).findByEmailAndPassword(eq(user.getEmail()), any(String.class));
            return;
        }
        fail("An EnfException was expected to be thrown (2)");
    }
}
