package services.oauth;

import models.RefreshToken;
import models.User;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import repositories.TokenRepository;
import services.AccessTokenCache;
import services.exceptions.InvalidTokenException;
import services.oauth.impl.SimpleTokenService;
import utils.UnitTest;

import javax.persistence.NoResultException;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static utils.TestConstants.FAKE_CLIENT_ID;
import static utils.TestConstants.WRONG_FAKE_CLIENT_ID;

/**
 * Created by Dan on 11/30/2016.
 */
public class SimpleTokenServiceTest extends UnitTest {

    @Mock
    private User user;
    @Mock
    private AccessTokenCache accessTokenCache;
    @Mock
    private TokenRepository repository;
    @InjectMocks
    private SimpleTokenService tokenService;

    @Override
    public void setUp() {
        AccessTokenCache.refreshTokenDaysUntilExpiration = 30;
    }

    @Test
    public void testCreatingNewRefreshToken() {
        when(repository.findByUserAndClient(eq(user), eq(FAKE_CLIENT_ID))).thenThrow(new NoResultException());
        tokenService.createRefreshToken(user, FAKE_CLIENT_ID);

        verify(repository, never()).deleteAndFlush(any(RefreshToken.class));
        verify(repository).save(any(RefreshToken.class));
        verify(accessTokenCache).addToCache(isNull(), any(RefreshToken.class));
    }

    @Test
    public void testCreatingNewRefreshTokenAndDeletingOld() {
        RefreshToken refreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
        when(repository.findByUserAndClient(eq(user), eq(FAKE_CLIENT_ID))).thenReturn(refreshToken);
        tokenService.createRefreshToken(user, FAKE_CLIENT_ID);

        verify(repository).deleteAndFlush(eq(refreshToken));
        verify(repository).save(any(RefreshToken.class));
        verify(accessTokenCache).addToCache(eq(refreshToken), any(RefreshToken.class));
    }

    @Test
    public void testGetUserOnValidToken() {
        RefreshToken refreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
        when(accessTokenCache.getUser(eq(refreshToken.getAccessToken()))).thenReturn(user);
        User returnedUser = tokenService.getUser(refreshToken.getAccessToken());

        assertEquals("Get user returned the wrong value", user, returnedUser);
        verify(accessTokenCache).getUser(eq(refreshToken.getAccessToken()));
    }

    @Test
    public void testGetUserOnInvalidToken() {
        RefreshToken refreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
        when(accessTokenCache.getUser(eq(refreshToken.getAccessToken()))).thenReturn(null);
        User returnedUser = tokenService.getUser(refreshToken.getAccessToken());

        assertEquals("Get user returned wrong value", null, returnedUser);
        verify(accessTokenCache).getUser(eq(refreshToken.getAccessToken()));
    }

    @Test
    public void testUpdatingRefreshTokenWithInvalidToken() {
        RefreshToken refreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
        when(repository.find(refreshToken.getToken())).thenReturn(null);
        try {
            tokenService.updateRefreshToken(refreshToken.getToken(), refreshToken.getClientId());
        } catch (InvalidTokenException e) {
            assertTrue("Invalid Token Exception has the wrong error message", e.getMessage().equals("Invalid Refresh Token"));
            verify(repository).find(refreshToken.getToken());
            return;
        }
        fail("An exception was expected to be thrown");
    }

    @Test
    public void testUpdatingRefreshTokenWithInvalidClientId() {
        RefreshToken refreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
        when(repository.find(refreshToken.getToken())).thenReturn(refreshToken);
        try {
            tokenService.updateRefreshToken(refreshToken.getToken(), WRONG_FAKE_CLIENT_ID);
        } catch (InvalidTokenException e) {
            assertTrue("Invalid Token Exception has the wrong error message", e.getMessage().equals("Invalid client ID"));
            verify(repository).find(refreshToken.getToken());
            return;
        }
        fail("An exception was expected to be thrown");
    }

    @Test
    public void testUpdatingRefreshTokenWithExpiredToken() {
        RefreshToken refreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
        refreshToken.setExpirationDate(new Date(new Date().getTime() - 10));
        when(repository.find(refreshToken.getToken())).thenReturn(refreshToken);
        try {
            tokenService.updateRefreshToken(refreshToken.getToken(), refreshToken.getClientId());
        } catch (InvalidTokenException e) {
            assertTrue("Invalid Token Exception has the wrong error message", e.getMessage().equals("Token has expired"));
            verify(repository).find(refreshToken.getToken());
            return;
        }
        fail("An exception was expected to be thrown");
    }

    @Test
    public void testUpdatingRefreshToken() {
        RefreshToken refreshToken = new RefreshToken(user, FAKE_CLIENT_ID);
        when(repository.find(refreshToken.getToken())).thenReturn(refreshToken);

        RefreshToken returnedToken = null;
        try {
            returnedToken = tokenService.updateRefreshToken(refreshToken.getToken(), refreshToken.getClientId());

            assertEquals(returnedToken.getClientId(), refreshToken.getClientId());
            assertEquals(returnedToken.getUser(), refreshToken.getUser());
            assertEquals(returnedToken.getExpirationDate(), refreshToken.getExpirationDate());
//            These asserts will not work until I can mock a static method
//            assertNotEquals(returnedToken.getToken(), refreshToken.getToken());
//            assertNotEquals(returnedToken.getAccessToken(), refreshToken.getAccessToken());
        } catch (InvalidTokenException e) {
            e.printStackTrace();
            fail("An exception should not be thrown");
        }

        verify(repository).find(refreshToken.getToken());
        verify(repository).deleteAndFlush(refreshToken);
        verify(repository).save(not(eq(refreshToken)));
        verify(accessTokenCache).addToCache(refreshToken, returnedToken);

    }
}
