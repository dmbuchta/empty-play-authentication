package services.userservice;

import models.User;
import org.junit.Test;
import play.Logger;
import services.exceptions.EnfException;

import javax.persistence.NoResultException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Dan on 11/19/2016.
 */
public class LoginTest extends UserServiceTest {

    private User user;

    @Override
    public void setUp() {
        super.setUp();
        user = new User();
        user.setEmail("testemail@playframework.com");
        user.setPassword("password");
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testGoodLoginWithEmailAndPassword() {
        Logger.debug("Running Good Login Test with email and password");
        when(mockedRepo.findByEmailAndPassword(eq(user.getEmail()), not(eq(user.getPassword())))).thenReturn(user);

        assertTrue("The user was expected to be returned", userService.login(user).equals(user));
        verify(mockedRepo).findByEmailAndPassword(eq(user.getEmail()), any(String.class));
    }

    @Test
    public void testBadLoginWithEmailAndPassword() {
        Logger.debug("Running Bad Login Test with email and password");
        when(mockedRepo.findByEmailAndPassword(eq(user.getEmail()), not(eq(user.getPassword())))).thenThrow(new NoResultException());

        try {
            userService.login(user);
        } catch (Exception e) {
            assertTrue("An EnfException was expected to be thrown (1)", e instanceof EnfException);
            verify(mockedRepo).findByEmailAndPassword(eq(user.getEmail()), any(String.class));
            return;
        }
        fail("An EnfException was expected to be thrown (2)");
    }

    @Test
    public void testLoginWithEmailOnly() {
        Logger.debug("Running Good Login Test with email only");
        when(mockedRepo.findByEmail(eq(user.getEmail()))).thenReturn(user);

        assertTrue("The user was expected to be returned", userService.findByEmail(user.getEmail()).equals(user));
        verify(mockedRepo).findByEmail(eq(user.getEmail()));
    }

    @Test
    public void testBadLoginWithEmailOnly() {
        Logger.debug("Running Bad Login Test with email only");
        when(mockedRepo.findByEmail(eq(user.getEmail()))).thenThrow(new NoResultException());

        try {
            userService.findByEmail(user.getEmail());
        } catch (Exception e) {
            assertTrue("An EnfException was expected to be thrown (1)", e instanceof EnfException);
            verify(mockedRepo).findByEmail(eq(user.getEmail()));
            return;
        }
        fail("An EnfException was expected to be thrown (2)");
    }
}
