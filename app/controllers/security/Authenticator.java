package controllers.security;

import models.User;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import services.caches.SessionCache;

import javax.inject.Inject;

/**
 * Created by Dan on 11/6/2016.
 */
public class Authenticator extends Security.Authenticator {

    private SessionCache sessionCache;

    @Inject
    public Authenticator(SessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

    @Override
    public String getUsername(Http.Context ctx) {
        String sessionId = ctx.session().get(SESSION_ID_PARAM);
        if (sessionId != null) {
            User user = sessionCache.getUser(sessionId);
            if (user != null) {
                ctx.args.put(CTX_USER_PARAM, user);
                // Reset the expiration on the user in the session cache
                sessionCache.addUserToCache(sessionId, user);
                return user.getEmail();
            }
        }
        return null;
    }

    @Override
    public Result onUnauthorized(Http.Context ctx) {
        return redirect(routes.SimpleLoginController.login());
    }

    public static void setSessionId(Http.Context ctx, String sessionId) {
        ctx.session().put(SESSION_ID_PARAM, sessionId);
    }

    public static boolean isUserLoggedIn(Http.Context ctx) {
        return ctx.args.get(CTX_USER_PARAM) != null;
    }

    public static final String SESSION_ID_PARAM = "SESSION_ID_PARAM";
    public static final String CTX_USER_PARAM = "CTX_USER_PARAM";
}
