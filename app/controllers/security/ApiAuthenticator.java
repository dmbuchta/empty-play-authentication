package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
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

    private TokenService tokenService;
    private Authenticator authenticator;

    @Inject
    public ApiAuthenticator(TokenService tokenService, Authenticator authenticator) {
        super();
        this.tokenService = tokenService;
        this.authenticator = authenticator;
    }

    @Override
    public String getUsername(Http.Context ctx) {
        // first check the session for the user
        String username = authenticator.getUsername(ctx);
        if (username != null) {
            return username;
        }

        // get the access token from the context
        String accessToken = ctx.request().getHeader(ACCESS_TOKEN_HEADER);
        if (accessToken == null) {
            Logger.warn("Access Token is not provided");
            return null;
        }
        ctx.args.put(CTX_ACCESS_TOKEN_PARAM, accessToken);

        User user = tokenService.getUser(accessToken);
        if (user == null) {
            Logger.warn("Access Token is invalid or expired");
            return null;
        }

        ctx.args.put(Authenticator.CTX_USER_PARAM, user);
        return user.getEmail();
    }

    @Override
    public Result onUnauthorized(Http.Context ctx) {
        ObjectNode json = Utils.createAjaxResponse(false);
        if (ctx.args.get(CTX_ACCESS_TOKEN_PARAM) != null) {
            json.put("message", "Invalid Token.");
        } else {
            json.put("message", "You must login to perform this action.");
        }
        return unauthorized(json);
    }

    // public to make testing easier
    public static final String ACCESS_TOKEN_HEADER = "Authorization";
    public static final String CTX_ACCESS_TOKEN_PARAM = "CTX_ACCESS_TOKEN_PARAM";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
}