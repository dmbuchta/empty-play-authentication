package services.caches;

import models.User;
import play.Logger;
import play.cache.CacheApi;
import play.cache.NamedCache;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Dan on 12/2/2016.
 */
@Singleton
public class SessionCache {

    private CacheApi cache;

    @Inject
    public SessionCache(@NamedCache("session-cache") CacheApi cache) {
        LOGGER.debug("Initializing Session Cache");
        this.cache = cache;
    }

    public User getUser(String sessionId) {
        return cache.get(sessionId);
    }

    public void addUserToCache(String sessionId, User user) {
        cache.set(sessionId, user, SESSION_INACTIVE_LENGTH);
    }

    public void removeFromCache(String sessionId) {
        cache.remove(sessionId);
    }

    // 1 hour
    private static final int SESSION_INACTIVE_LENGTH = 60 * 60;

    private static final Logger.ALogger LOGGER = Logger.of(SessionCache.class);

}
