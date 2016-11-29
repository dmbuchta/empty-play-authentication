package controllers.security;

import actions.CheckGoogleConfigAction;
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
import services.login.impl.GoogleLoginService;
import utils.Utils;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public class GoogleLoginController extends BaseController implements LoginController {

    private FormFactory formFactory;
    private LoginService loginService;

    @Inject
    public GoogleLoginController(FormFactory formFactory, @Named("google") LoginService loginService) {
        this.formFactory = formFactory;
        this.loginService = loginService;
    }

    @Override
    @With(CheckGoogleConfigAction.class)
    public CompletionStage<Result> login() {
        Form<GoogleLoginService.GoogleLoginForm> loginForm = formFactory.form(GoogleLoginService.GoogleLoginForm.class).bindFromRequest();
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

            Logger.debug("{}", ((Throwable) throwable).getCause());

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
    @With(CheckGoogleConfigAction.class)
    public CompletionStage<Result> apiLogin() {
        return CompletableFuture.completedFuture(status(NOT_IMPLEMENTED));
    }
}
