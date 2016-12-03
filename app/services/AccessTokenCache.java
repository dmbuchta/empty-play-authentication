package services;

import models.RefreshToken;
import models.User;
import play.Configuration;
import play.Logger;
import play.cache.CacheApi;
import play.cache.NamedCache;
import utils.Configs;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by Dan on 11/29/2016.
 */
@Singleton
public class AccessTokenCache {

    private CacheApi cache;

    @Inject
    public AccessTokenCache(@NamedCache("token-cache") CacheApi cache, Configuration configuration) {
        Logger.debug("Initializing Token Cache");

        accessTokenSecondsUntilExpiration = configuration.getInt(Configs.API_ACCESS_TOKEN_LENGTH, DEFAULT_ACCESS_TOKEN_EXPIRATION_IN_SECONDS);
        Logger.debug("Setting Access Token length to {} seconds", accessTokenSecondsUntilExpiration);

        refreshTokenDaysUntilExpiration = configuration.getInt(Configs.API_REFRESH_TOKEN_LENGTH, DEFAULT_REFRESH_TOKEN_EXPIRATION_IN_DAYS);
        Logger.debug("Setting Refresh Token length to {} days", refreshTokenDaysUntilExpiration);

        tokenCharLength = configuration.getInt(Configs.API_TOKEN_SIZE, DEFAULT_TOKEN_CHAR_LENGTH);
        Logger.debug("Setting Token Size to {} chars", tokenCharLength);

        this.cache = cache;
    }

    public void addToCache(RefreshToken oldRefreshToken, RefreshToken refreshToken) {
        if (oldRefreshToken != null) {
            Logger.debug("Removing old accessToken");
            cache.remove(oldRefreshToken.getAccessToken());
        }
        cache.set(refreshToken.getAccessToken(), refreshToken.getUser(), accessTokenSecondsUntilExpiration);
    }

    public User getUser(String accessToken) {
        User user = cache.get(accessToken);
        Logger.debug("Found User '{}' in cache", user);
        return user;
    }

    public static String generateRandomToken() {
        return new BigInteger(tokenCharLength * 5, secureRandom).toString(32);
    }

    // (((1 second x 60 = 1 minute) x 60) = 1 hour) x 2 = 2 hours
    private static final int DEFAULT_ACCESS_TOKEN_EXPIRATION_IN_SECONDS = (((1 * 60) * 60) * 2);
    private static final int DEFAULT_REFRESH_TOKEN_EXPIRATION_IN_DAYS = 14;
    private static final int DEFAULT_TOKEN_CHAR_LENGTH = 255;
    private static final SecureRandom secureRandom = new SecureRandom();

    public static int refreshTokenDaysUntilExpiration = DEFAULT_REFRESH_TOKEN_EXPIRATION_IN_DAYS;
    private static int accessTokenSecondsUntilExpiration = DEFAULT_ACCESS_TOKEN_EXPIRATION_IN_SECONDS;
    private static int tokenCharLength = DEFAULT_TOKEN_CHAR_LENGTH;
}
