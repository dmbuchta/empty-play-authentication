package services.oauth.impl;

import models.RefreshToken;
import models.User;
import play.Logger;
import repositories.TokenRepository;
import services.caches.AccessTokenCache;
import services.exceptions.InvalidTokenException;
import services.login.impl.SimpleLoginService;
import services.oauth.TokenService;

import javax.inject.Inject;
import javax.persistence.NoResultException;

/**
 * Created by Dan on 11/29/2016.
 */
public class SimpleTokenService implements TokenService {

    private AccessTokenCache tokenCache;
    private TokenRepository repository;

    @Inject
    public SimpleTokenService(TokenRepository repository, AccessTokenCache tokenCache) {
        this.repository = repository;
        this.tokenCache = tokenCache;
    }

    @Override
    public RefreshToken createRefreshToken(User user, String clientId) {
        LOGGER.debug("Creating Refresh Token");
        RefreshToken oldRefreshToken = null;
        try {
            oldRefreshToken = repository.findByUserAndClient(user, clientId);
            repository.deleteAndFlush(oldRefreshToken);
        } catch (NoResultException e) {
            // do nothing
        }
        RefreshToken refreshToken = new RefreshToken(user, clientId);
        repository.save(refreshToken);
        tokenCache.addToCache(oldRefreshToken, refreshToken);
        return refreshToken;
    }

    @Override
    public RefreshToken updateRefreshToken(String oldRefreshTokenStr, String clientId) throws InvalidTokenException {
        LOGGER.debug("Updating Refresh Token");
        RefreshToken oldRefreshToken = repository.find(oldRefreshTokenStr);
        if (oldRefreshToken == null) {
            throw new InvalidTokenException("Invalid Refresh Token");
        }
        if (!oldRefreshToken.getClientId().equals(clientId)) {
            throw new InvalidTokenException("Invalid client ID");
        }
        if (oldRefreshToken.isExpired()) {
            throw new InvalidTokenException("Token has expired");
        }
        RefreshToken refreshToken = new RefreshToken(oldRefreshToken);
        repository.deleteAndFlush(oldRefreshToken);
        repository.save(refreshToken);
        tokenCache.addToCache(oldRefreshToken, refreshToken);
        return refreshToken;
    }

    @Override
    public User getUser(String accessToken) {
        return tokenCache.getUser(accessToken);
    }

    private static final Logger.ALogger LOGGER = Logger.of(SimpleTokenService.class);
}
