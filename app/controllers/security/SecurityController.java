package controllers.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.BaseController;
import models.User;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PSQLException;
import play.Configuration;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Results;
import services.UserService;
import services.exceptions.DuplicateEntityException;
import services.exceptions.EnfException;
import utils.Configs;
import utils.Utils;

import javax.inject.Inject;

/**
 * Created by Dan on 11/6/2016.
 */
public class SecurityController extends BaseController {

    private FormFactory formFactory;
    private UserService userService;
    private String ssoClientId;
    private String fbAppId;

    @Inject
    public SecurityController(FormFactory formFactory, UserService userService, Configuration configuration) {
        this.formFactory = formFactory;
        this.userService = userService;
        this.ssoClientId = configuration.getString(Configs.GOOGLE_CLIENT_ID);
        String appId = configuration.getString(Configs.FB_APP_ID);
        if (StringUtils.isNoneBlank(appId, configuration.getString(Configs.FB_APP_SECRET))) {
            this.fbAppId = appId;
        }
    }

    public Result showLoginPage() {
        if (Authenticator.isUserLoggedIn(ctx())) {
            return redirect(controllers.secured.routes.HomeController.index());
        }
        return ok(views.html.login.render(formFactory.form(User.class), formFactory.form(NewUserForm.class), ssoClientId, fbAppId));
    }

    @Transactional(readOnly = true)
    public Result login() {
        Form<User> loginForm = formFactory.form(User.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return Results.ok(views.html.login.render(loginForm, formFactory.form(NewUserForm.class), ssoClientId, fbAppId));
        }
        try {
            User user = loginForm.get();
            user = userService.login(user);
            Authenticator.setUser(ctx(), user);
            return redirect(controllers.secured.routes.HomeController.index());
        } catch (EnfException e) {
            return Results.ok(views.html.login.render(loginForm, formFactory.form(NewUserForm.class), ssoClientId, fbAppId));
        }
    }

    @Transactional
    public Result createAccount() {
        Form<NewUserForm> newUserForm = formFactory.form(NewUserForm.class).bindFromRequest();
        if (newUserForm.hasErrors()) {
            return ok(Utils.createAjaxResponse(newUserForm));
        }
        ObjectNode json;
        try {
            User user = userService.createNewUser(newUserForm.get());
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

    public Result logout() {
        Authenticator.logout(ctx());
        return redirect(routes.SecurityController.showLoginPage());
    }

    public static class NewUserForm {

        @Constraints.Required
        @Constraints.MinLength(3)
        @Constraints.MaxLength(256)
        @Constraints.Email
        private String newEmail;

        @Constraints.Required
        @Constraints.MinLength(6)
        @Constraints.MaxLength(256)
        private String newPassword;

        @Constraints.Required
        private String confirmPassword;

        public String getNewEmail() {
            return newEmail;
        }

        public void setNewEmail(String newEmail) {
            this.newEmail = newEmail;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }

}
