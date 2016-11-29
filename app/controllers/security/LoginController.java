package controllers.security;

import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public interface LoginController {

    CompletionStage<Result> login();

    CompletionStage<Result> apiLogin();
}
