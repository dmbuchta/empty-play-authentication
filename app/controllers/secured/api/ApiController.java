package controllers.secured.api;

import controllers.BaseController;
import controllers.security.ApiAuthenticator;
import play.mvc.Security;

/**
 * Created by Dan on 11/29/2016.
 */
@Security.Authenticated(ApiAuthenticator.class)
public abstract class ApiController extends BaseController {
}
