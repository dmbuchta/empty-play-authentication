package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import play.Logger;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import services.SessionCache;
import services.oauth.TokenService;
import utils.Utils;

import javax.inject.Inject;

/**
 * Created by Dan on 11/29/2016.
 */
public class ApiAuthenticator extends Security.Authenticator {

    public static final String ACCESS_TOKEN_HEADER = "Authorization";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";

    private TokenService tokenService;
    private SessionCache sessionCache;
    private boolean isAccessTokenProvided;

    @Inject
    public ApiAuthenticator(TokenService tokenService, SessionCache sessionCache) {
        super();
        this.tokenService = tokenService;
        this.sessionCache = sessionCache;
    }

    @Override
    public String getUsername(Http.Context ctx) {
        User user = null;
        String sessionId = ctx.session().get(Authenticator.SESSION_ID_PARAM);
        if (sessionId != null) {
            user = sessionCache.getUser(sessionId);
        }
        if (user == null) {
            String accessToken = getAccessToken(ctx);
            if (accessToken == null) {
                Logger.warn("Access Token is not provided");
                return null;
            }
            isAccessTokenProvided = true;
            user = tokenService.getUser(accessToken);
            if (user == null) {
                Logger.warn("Access Token is invalid or expired");
                return null;
            }
        }
        ctx.args.put(Authenticator.CTX_USER_PARAM, user);
        return user.getEmail();
    }

    @Override
    public Result onUnauthorized(Http.Context ctx) {
        ObjectNode json = Utils.createAjaxResponse(false);
        if (isAccessTokenProvided) {
            json.put("message", "Invalid Token.");
        } else {
            json.put("message", "You must login to perform this action.");
        }
        return unauthorized(json);
    }

    public static String getAccessToken(Http.Context ctx) {
        return ctx.request().getHeader(ACCESS_TOKEN_HEADER);
    }

}