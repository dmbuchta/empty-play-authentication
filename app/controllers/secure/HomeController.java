package controllers.secure;

import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.mvc.Result;

import javax.inject.Inject;

public class HomeController extends UserRequiredController {

    @Inject
    public HomeController(FormFactory formFactory, JPAApi jpaApi) {
        super(formFactory);
    }

    public Result index() {
        return ok(views.html.index.render());
    }

}
