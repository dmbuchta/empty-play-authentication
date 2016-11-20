package controllers.secure;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import org.postgresql.util.PSQLException;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import services.UserService;
import services.exceptions.DuplicateEntityException;
import services.exceptions.EnfException;
import utils.Utils;

import javax.inject.Inject;

/**
 * Created by Dan on 11/6/2016.
 */
public class SecurityController extends Controller {

    private FormFactory formFactory;
    private UserService userService;

    @Inject
    public SecurityController(FormFactory formFactory, UserService userService) {
        this.formFactory = formFactory;
        this.userService = userService;
    }

    public Result showLoginPage() {
        return ok(views.html.login.render(formFactory.form(User.class), formFactory.form(NewUserForm.class)));
    }

    @Transactional(readOnly = true)
    public Result login() {
        Form<User> loginForm = formFactory.form(User.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return Results.ok(views.html.login.render(loginForm, formFactory.form(NewUserForm.class)));
        }
        try {
            User user = loginForm.get();
            user = userService.login(user);
            Authenticator.setUser(ctx(), user);
            return redirect(routes.HomeController.index());
        } catch (EnfException e) {
            return Results.ok(views.html.login.render(loginForm, formFactory.form(NewUserForm.class)));
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
            json.put("url", routes.HomeController.index().url());
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
        public String newEmail;

        @Constraints.Required
        @Constraints.MinLength(6)
        @Constraints.MaxLength(256)
        public String newPassword;

        @Constraints.Required
        public String confirmPassword;

    }

}
