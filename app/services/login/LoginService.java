package services.login;

import models.User;
import play.data.Form;

import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public interface LoginService<U> {

    CompletionStage<User> login(Form<U> form);
}
