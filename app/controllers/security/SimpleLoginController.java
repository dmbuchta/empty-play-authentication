package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.BaseController;
import models.User;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PSQLException;
import play.Configuration;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.AccountService;
import services.exceptions.DuplicateEntityException;
import services.exceptions.EnfException;
import services.login.LoginService;
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
    private AccountService accountService;
    private final String googleClientId;
    private final String facebookAppId;

    @Inject
    public SimpleLoginController(FormFactory formFactory, LoginService loginService, AccountService accountService, Configuration configuration) {
        this.formFactory = formFactory;
        this.loginService = loginService;
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
                return redirect(controllers.secured.routes.HomeController.index());
            });
        } catch (EnfException e) {
            return CompletableFuture.completedFuture(
                    Results.ok(views.html.login.render(loginForm, formFactory.form(AccountService.NewUserForm.class), googleClientId, facebookAppId)));
        }
    }

    @Override
    public CompletionStage<Result> apiLogin() {
        return CompletableFuture.completedFuture(status(NOT_IMPLEMENTED));
    }

    public Result showLoginPage() {
        if (Authenticator.isUserLoggedIn(ctx())) {
            return redirect(controllers.secured.routes.HomeController.index());
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
            json.put("url", controllers.secured.routes.HomeController.index().url());
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
}
