package services.accesstokencache;

import models.RefreshToken;
import models.User;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.Configuration;
import play.cache.CacheApi;
import services.AccessTokenCache;
import utils.UnitTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static utils.TestConstants.FAKE_CLIENT_ID;

/**
 * Created by Dan on 11/30/2016.
 */
public class AccessTokenTest extends UnitTest {

    private RefreshToken refreshToken;

    @Mock
    private User user;
    @Mock
    private CacheApi cache;
    @Mock
    private Configuration configuration;
    @InjectMocks
    private AccessTokenCache accessTokenCache;

    @Override
    public void setUp() {
        super.setUp();
        refreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
    }

    @Test
    public void testAddingToCacheRemovesOldToken() {
        RefreshToken oldRefreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
        accessTokenCache.addToCache(oldRefreshToken, refreshToken);
        verify(cache).remove(eq(oldRefreshToken.getAccessToken()));
        verify(cache).set(eq(refreshToken.getAccessToken()), eq(user), anyInt());
    }

    @Test
    public void testAddingToCacheWithoutOldToken() {
        accessTokenCache.addToCache(null, refreshToken);
        ArgumentCaptor<RefreshToken> tokenParam = ArgumentCaptor.forClass(RefreshToken.class);
        verify(cache, never()).remove(anyString());
        verify(cache).set(eq(refreshToken.getAccessToken()), eq(user), anyInt());
    }

    @Test
    public void testGettingUserWithToken() {
        when(cache.get(refreshToken.getAccessToken())).thenReturn(user);
        User returnedUser = accessTokenCache.getUser(refreshToken.getAccessToken());
        assertEquals("Cache did not return user", returnedUser, user);
    }

    @Test
    public void testGettingUserWithTokenWhenNoUserExists() {
        User returnedUser = accessTokenCache.getUser(refreshToken.getAccessToken());
        assertEquals("Cache did not return user", returnedUser, null);
    }
}
