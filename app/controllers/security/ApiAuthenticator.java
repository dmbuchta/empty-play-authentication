package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
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
    private boolean isAccessTokenProvided;

    @Inject
    public ApiAuthenticator(TokenService tokenService) {
        super();
        this.tokenService = tokenService;
    }

    @Override
    public String getUsername(Http.Context ctx) {
        if (!Authenticator.isUserLoggedIn(ctx)) {
            String accessToken = getAccessToken(ctx);
            if (accessToken == null) {
                Logger.warn("Access Token is not provided");
                return null;
            }
            isAccessTokenProvided = true;
            if (!tokenService.isValidAccessToken(accessToken)) {
                Logger.warn("Access Token is invalid or expired");
                return null;
            }
        }
        return "1";
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