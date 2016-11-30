package controllers.security;

import actions.CheckApiClientAction;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.BaseController;
import models.RefreshToken;
import models.User;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.postgresql.util.PSQLException;
import play.Configuration;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;
import services.AccountService;
import services.exceptions.DuplicateEntityException;
import services.exceptions.EnfException;
import services.exceptions.InvalidTokenException;
import services.login.LoginService;
import services.oauth.TokenService;
import utils.Configs;
import utils.Utils;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public class SimpleLoginController extends BaseController implements LoginController {

    private FormFactory formFactory;
    private LoginService loginService;
    private TokenService tokenService;
    private AccountService accountService;
    private final String googleClientId;
    private final String facebookAppId;

    @Inject
    public SimpleLoginController(FormFactory formFactory, LoginService loginService, TokenService tokenService,
                                 AccountService accountService, Configuration configuration) {
        this.formFactory = formFactory;
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.accountService = accountService;
        this.googleClientId = configuration.getString(Configs.GOOGLE_CLIENT_ID);
        String appId = configuration.getString(Configs.FB_APP_ID);
        if (StringUtils.isNoneBlank(appId, configuration.getString(Configs.FB_APP_SECRET))) {
            this.facebookAppId = appId;
        } else {
            facebookAppId = null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CompletionStage<Result> login() {
        Form<User> loginForm = formFactory.form(User.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return CompletableFuture.completedFuture(ok(views.html.login.render(loginForm, formFactory.form(AccountService.NewUserForm.class), googleClientId, facebookAppId)));
        }
        final Http.Context ctx = ctx();
        try {
            return loginService.login(loginForm).thenApplyAsync(user -> {
                Authenticator.setUser(ctx, (User) user);
                return redirect(controllers.secured.html.routes.UserController.index());
            });
        } catch (EnfException e) {
            return CompletableFuture.completedFuture(
                    Results.ok(views.html.login.render(loginForm, formFactory.form(AccountService.NewUserForm.class), googleClientId, facebookAppId)));
        }
    }

    @Override
    @Transactional
    @With(CheckApiClientAction.class)
    public CompletionStage<Result> apiLogin() {
        Form<User> loginForm = formFactory.form(User.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return CompletableFuture.completedFuture(ok(Utils.createAjaxResponse(loginForm)));
        }
        try {
            final String clientId = (String) ctx().args.get(CheckApiClientAction.CLIENT_REQUEST_ARG);
            return loginService.login(loginForm).thenApply(userObj -> {
                User user = (User) userObj;
                RefreshToken refreshToken = tokenService.createRefreshToken(user, clientId);
                ObjectNode responseJson = Utils.createAjaxResponse(true);
                responseJson.put("id", user.getId());
                responseJson.put(ApiAuthenticator.REFRESH_TOKEN, refreshToken.getToken());
                responseJson.put(ApiAuthenticator.ACCESS_TOKEN, refreshToken.getAccessToken());
                return ok(responseJson);
            });
        } catch (EnfException e) {
            ObjectNode json = Utils.createAjaxResponse(false);
            json.put("message", "There was a problem with your login");
            return CompletableFuture.completedFuture(ok(json));
        }
    }

    @Transactional
    @With(CheckApiClientAction.class)
    public Result refreshToken() {
        Form<RefreshTokenForm> form = formFactory.form(RefreshTokenForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return unauthorized(Utils.createAjaxResponse(form));
        }
        String refreshTokenStr = form.get().getRefreshToken();
        String clientId = (String) ctx().args.get(CheckApiClientAction.CLIENT_REQUEST_ARG);
        ObjectNode responseJson;
        try {
            RefreshToken refreshToken = tokenService.updateRefreshToken(refreshTokenStr, clientId);
            responseJson = Utils.createAjaxResponse(true);
            responseJson.put(ApiAuthenticator.REFRESH_TOKEN, refreshToken.getToken());
            responseJson.put(ApiAuthenticator.ACCESS_TOKEN, refreshToken.getAccessToken());
            return ok(responseJson);
        } catch (InvalidTokenException e) {
            responseJson = Utils.createAjaxResponse(false);
            responseJson.put("message", "Invalid Token.");
            return unauthorized(responseJson);
        }
    }

    public Result showLoginPage() {
        if (Authenticator.isUserLoggedIn(ctx())) {
            return redirect(controllers.secured.html.routes.UserController.index());
        }
        return ok(views.html.login.render(formFactory.form(User.class), formFactory.form(AccountService.NewUserForm.class), googleClientId, facebookAppId));
    }

    public Result logout() {
        Authenticator.logout(ctx());
        return redirect(routes.SimpleLoginController.showLoginPage());
    }


    @Transactional
    public Result createAccount() {
        Form<AccountService.NewUserForm> newUserForm = formFactory.form(AccountService.NewUserForm.class).bindFromRequest();
        if (newUserForm.hasErrors()) {
            return ok(Utils.createAjaxResponse(newUserForm));
        }
        ObjectNode json;
        try {
            User user = accountService.createNewAccount(newUserForm.get());
            Authenticator.setUser(ctx(), user);
            json = Utils.createAjaxResponse(true);
            // TODO: REDIRECT TO ORIGINAL REQUEST URL!!
            json.put("url", controllers.secured.html.routes.UserController.index().url());
        } catch (DuplicateEntityException e) {
            json = Utils.createAjaxResponse(false);
            json.put("message", "There is already an account with this email address.");
        } catch (PSQLException e) {
            // This should never happen
            json = Utils.createAjaxResponse(false);
            json.put("message", "An error occurred creating account.");
        }
        return ok(json);
    }

    public static class RefreshTokenForm {
        @Constraints.Required
        @NotEmpty
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
