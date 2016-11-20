package controllers.secure;

import models.User;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Created by Dan on 11/6/2016.
 */
public class Authenticator extends Security.Authenticator {

    private static final String USER_SESSION_PARAM = "uId";

    @Override
    public String getUsername(Http.Context ctx) {
        return ctx.session().get(USER_SESSION_PARAM);
    }

    @Override
    public Result onUnauthorized(Http.Context ctx) {
        return redirect(routes.SecurityController.login());
    }

    static void setUser(Http.Context ctx, User user) {
        ctx.session().put(USER_SESSION_PARAM, user.getId() + "");
    }

    static void logout(Http.Context ctx) {
        ctx.session().remove(USER_SESSION_PARAM);
    }

    public static boolean isUserLoggedIn(Http.Context ctx) {
        return !StringUtils.isBlank(ctx.session().get(USER_SESSION_PARAM));
    }
}
