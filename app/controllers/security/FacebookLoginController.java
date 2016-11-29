package controllers.security;

import actions.CheckFacebookConfigAction;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.name.Named;
import controllers.BaseController;
import models.User;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import services.exceptions.EnfException;
import services.login.LoginService;
import services.login.impl.FacebookLoginService;
import utils.Utils;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public class FacebookLoginController extends BaseController implements LoginController {

    private FormFactory formFactory;
    private LoginService loginService;

    @Inject
    public FacebookLoginController(FormFactory formFactory, @Named("facebook") LoginService loginService) {
        this.formFactory = formFactory;
        this.loginService = loginService;
    }

    @Override
    @With(CheckFacebookConfigAction.class)
    public CompletionStage<Result> login() {
        Form<FacebookLoginService.FacebookLoginForm> loginForm = formFactory.form(FacebookLoginService.FacebookLoginForm.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return CompletableFuture.completedFuture(ok(Utils.createAjaxResponse(loginForm)));
        }
        final Http.Context ctx = ctx();
        return loginService.login(loginForm).thenApplyAsync(user -> {
            Authenticator.setUser(ctx, (User) user);
            ObjectNode responseJson = Utils.createAjaxResponse(true);
            responseJson.put("url", controllers.secured.routes.HomeController.index().url());
            return ok(responseJson);
        }).exceptionally(throwable -> {
            ObjectNode json = Utils.createAjaxResponse(false);
            if (((Throwable) throwable).getCause() instanceof EnfException) {
                json.put("message", "No account");
                json.put("email", ((Throwable) throwable).getCause().getMessage());
            } else {
                Logger.error("An error occurred using Facebook Sign In", throwable);
                json.put("message", "There was a problem with your login");
            }
            return ok(json);
        });
    }

    @Override
    @With(CheckFacebookConfigAction.class)
    public CompletionStage<Result> apiLogin() {
        return CompletableFuture.completedFuture(status(NOT_IMPLEMENTED));
    }
}
