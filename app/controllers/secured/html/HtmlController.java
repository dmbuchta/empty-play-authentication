package controllers.secured.html;

import controllers.BaseController;
import controllers.security.Authenticator;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Security;

/**
 * Created by Dan on 11/18/2016.
 */
@Security.Authenticated(Authenticator.class)
public abstract class HtmlController extends BaseController {

    protected FormFactory formFactory;

    public HtmlController(FormFactory formFactory) {
        this.formFactory = formFactory;
    }
}
