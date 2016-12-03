package services.sessioncache;

import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.cache.CacheApi;
import services.SessionCache;
import utils.UnitTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestConstants.FAKE_SESSION_ID;

/**
 * Created by Dan on 12/2/2016.
 */
public class SessionCacheTest extends UnitTest {

    @Mock
    private User user;
    @Mock
    private CacheApi cache;
    @InjectMocks
    private SessionCache sessionCache;

    @Test
    public void testGettingUserFromCache() {
        when(cache.get(FAKE_SESSION_ID)).thenReturn(user);
        User returnedUser = sessionCache.getUser(FAKE_SESSION_ID);
        assertEquals("Returned user is not correct", user, returnedUser);
        verify(cache).get(FAKE_SESSION_ID);
    }

    @Test
    public void testGettingUserFromCacheWithNoUser() {
        User returnedUser = sessionCache.getUser(FAKE_SESSION_ID);
        assertEquals("Returned user is not null", null, returnedUser);
        verify(cache).get(eq(FAKE_SESSION_ID));
    }

    @Test
    public void testAddingUserToCache() {
        sessionCache.addUserToCache(FAKE_SESSION_ID, user);
        verify(cache).set(eq(FAKE_SESSION_ID), eq(user), anyInt());
    }

    @Test
    public void testRemovingFromCache() {
        sessionCache.removeFromCache(FAKE_SESSION_ID);
        verify(cache).remove(eq(FAKE_SESSION_ID));
    }

}
