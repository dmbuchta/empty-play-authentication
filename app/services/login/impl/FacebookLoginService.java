package services.login.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.name.Named;
import models.User;
import org.hibernate.validator.constraints.NotEmpty;
import play.Configuration;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import repositories.UserRepository;
import services.exceptions.EnfException;
import services.login.LoginService;
import utils.Configs;
import utils.Utils;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public class FacebookLoginService implements LoginService<FacebookLoginService.FacebookLoginForm> {

    // public to make testing easier
    public static final String FB_TOKEN_ENDPOINT = "https://graph.facebook.com/debug_token";
    public static final String FB_USER_EMAIL_ENDPOINT = "https://graph.facebook.com/v2.8/";
    public static final String INVALID_TOKEN_RESPONSE = "Invalid Facebook Token";

    private UserRepository repository;
    private WSClient wsClient;
    private final String appId;
    private final String accessToken;

    @Inject
    public FacebookLoginService(@Named("unbound") UserRepository repository, WSClient wsClient, Configuration configuration) {
        this.repository = repository;
        this.wsClient = wsClient;
        this.appId = configuration.getString(Configs.FB_APP_ID);
        this.accessToken = appId + "|" + configuration.getString(Configs.FB_APP_SECRET);
    }

    @Override
    public CompletionStage<User> login(Form<FacebookLoginForm> form) {
        Logger.debug("Doing a Facebook User login");
        FacebookLoginForm loginForm = form.get();
        return verifyFacebookToken(loginForm.getInput_token())
                .thenCombineAsync(getEmailFromFacebook(loginForm.getUserID()), (isValid, email) -> {
                    if (isValid) {
                        try {
                            return repository.findByEmail(email);
                        } catch (NoResultException e) {
                            Logger.debug("Entity Not Found!");
                            throw new EnfException(email);
                        }
                    }
                    throw new RuntimeException(INVALID_TOKEN_RESPONSE);
                });
    }

    private CompletionStage<Boolean> verifyFacebookToken(String token) {
        return wsClient.url(FB_TOKEN_ENDPOINT)
                .setQueryParameter("input_token", token)
                .setQueryParameter("access_token", accessToken)
                .get()
                .thenApply(Utils::debugResponse)
                .thenApply(WSResponse::asJson)
                .thenApply(jsonNode -> {
                    ObjectNode json = (jsonNode.has("data") ? (ObjectNode) jsonNode.get("data") : null);
                    if (json != null && appId.equals(json.get("app_id").asText())) {
                        return json.has("is_valid") && json.get("is_valid").asBoolean();
                    }
                    Logger.warn("App ID does not match. Someone is doing something very suspicious.");
                    return false;
                });
    }

    private CompletionStage<String> getEmailFromFacebook(String userId) {
        return wsClient.url(FB_USER_EMAIL_ENDPOINT + userId)
                .setQueryParameter("fields", "email")
                .setQueryParameter("access_token", accessToken)
                .get()
                .thenApply(Utils::debugResponse)
                .thenApply(WSResponse::asJson)
                .thenApply(jsonNode -> jsonNode.get("email").asText());
    }

    public static class FacebookLoginForm {
        @NotEmpty
        @Constraints.Required
        private String input_token;
        @NotEmpty
        @Constraints.Required
        private String userID;

        public String getInput_token() {
            return input_token;
        }

        public void setInput_token(String input_token) {
            this.input_token = input_token;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }
    }
}
