package controllers.secured.api;

import play.mvc.Result;

/**
 * Created by Dan on 11/29/2016.
 */
public class UserApiController extends ApiController {

    public Result index() {
        return ok("YOU HAVE ACCESS!");
    }

}
