package controllers.secured;

import controllers.security.Authenticator;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Security;

/**
 * Created by Dan on 11/18/2016.
 */
@Security.Authenticated(Authenticator.class)
public abstract class UserRequiredController extends Controller {

    protected FormFactory formFactory;

    public UserRequiredController(FormFactory formFactory) {
        this.formFactory = formFactory;
    }
}
