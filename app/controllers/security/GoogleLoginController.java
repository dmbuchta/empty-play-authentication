package controllers.security;

import actions.CheckApiClientAction;
import actions.CheckGoogleConfigAction;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.name.Named;
import controllers.BaseController;
import models.RefreshToken;
import models.User;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import services.caches.SessionCache;
import services.exceptions.EnfException;
import services.login.LoginService;
import services.login.impl.GoogleLoginService;
import services.oauth.TokenService;
import utils.Utils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public class GoogleLoginController extends BaseController implements LoginController {

    private JPAApi jpaApi;
    private FormFactory formFactory;
    private LoginService loginService;
    private TokenService tokenService;
    private SessionCache sessionCache;

    @Inject
    public GoogleLoginController(FormFactory formFactory, @Named("google") LoginService loginService,
                                 TokenService tokenService, JPAApi jpaApi, SessionCache sessionCache) {
        this.formFactory = formFactory;
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.jpaApi = jpaApi;
        this.sessionCache = sessionCache;
    }

    @Override
    @With(CheckGoogleConfigAction.class)
    public CompletionStage<Result> login() {
        Form<GoogleLoginService.GoogleLoginForm> loginForm = formFactory.form(GoogleLoginService.GoogleLoginForm.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return CompletableFuture.completedFuture(ok(Utils.createAjaxResponse(loginForm)));
        }
        final Http.Context ctx = ctx();
        return loginService.login(loginForm).thenApply(user -> {
            String sessionId = UUID.randomUUID().toString();
            Authenticator.setSessionId(ctx, sessionId);
            sessionCache.addUserToCache(sessionId, (User) user);
            ObjectNode responseJson = Utils.createAjaxResponse(true);
            responseJson.put("url", controllers.secured.html.routes.UserController.index().url());
            return ok(responseJson);
        }).exceptionally(throwable -> {
            ObjectNode json = Utils.createAjaxResponse(false);
            if (((Throwable) throwable).getCause() instanceof EnfException) {
                json.put("message", "No account");
            } else {
                Logger.error("An error occurred using Google Sign In", throwable);
                json.put("message", "There was a problem with your login");
            }
            return ok(json);
        });
    }

    @Override
    @With({CheckGoogleConfigAction.class, CheckApiClientAction.class})
    public CompletionStage<Result> apiLogin() {
        Form<GoogleLoginService.GoogleLoginForm> loginForm = formFactory.form(GoogleLoginService.GoogleLoginForm.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return CompletableFuture.completedFuture(ok(Utils.createAjaxResponse(loginForm)));
        }
        final String clientId = (String) ctx().args.get(CheckApiClientAction.CLIENT_REQUEST_ARG);
        return loginService.login(loginForm).thenApply(userObj -> {
            User user = (User) userObj;
            RefreshToken refreshToken = jpaApi.withTransaction(() -> tokenService.createRefreshToken(user, clientId));
            ObjectNode responseJson = Utils.createAjaxResponse(true);
            responseJson.put("id", user.getId());
            responseJson.put(ApiAuthenticator.REFRESH_TOKEN, refreshToken.getToken());
            responseJson.put(ApiAuthenticator.ACCESS_TOKEN, refreshToken.getAccessToken());
            return ok(responseJson);
        }).exceptionally(throwable -> {
            ObjectNode json = Utils.createAjaxResponse(false);
            if (((Throwable) throwable).getCause() instanceof EnfException) {
                json.put("message", "No account");
            } else {
                Logger.error("An error occurred using Google Sign In", throwable);
                json.put("message", "There was a problem with your login");
            }
            return ok(json);
        });
    }
}
