package controllers.secured.html;

import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.mvc.Result;

import javax.inject.Inject;

public class UserController extends HtmlController {

    @Inject
    public UserController(FormFactory formFactory, JPAApi jpaApi) {
        super(formFactory);
    }

    public Result index() {
        return ok(views.html.index.render());
    }

}
