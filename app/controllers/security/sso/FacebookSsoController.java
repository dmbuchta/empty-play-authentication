package controllers.security.sso;

import actions.CheckFacebookConfigAction;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.security.Authenticator;
import models.User;
import org.hibernate.validator.constraints.NotEmpty;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import services.UserService;
import services.exceptions.EnfException;
import utils.Utils;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/26/2016.
 */
public class FacebookSsoController extends Controller {

    private static final String FB_TOKEN_ENDPOINT = "https://graph.facebook.com/debug_token";
    private static final String FB_USER_EMAIL_ENDPOINT = "https://graph.facebook.com/v2.8/";

    private JPAApi jpaApi;
    private FormFactory formFactory;
    private UserService userService;
    private WSClient wsClient;

    @Inject
    public FacebookSsoController(JPAApi jpaApi, FormFactory formFactory, UserService userService, WSClient wsClient) {
        this.jpaApi = jpaApi;
        this.formFactory = formFactory;
        this.userService = userService;
        this.wsClient = wsClient;
    }

    @Transactional(readOnly = true)
    @With(CheckFacebookConfigAction.class)
    public CompletionStage<Result> login() {
        Form<FbSsoForm> loginForm = formFactory.form(FbSsoForm.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return CompletableFuture.completedFuture(ok(Utils.createAjaxResponse(loginForm)));
        }
        final Http.Context ctx = ctx();
        final String accessToken = (String) ctx.args.get(CheckFacebookConfigAction.ACCESS_TOKEN);
        final String appId = (String) ctx.args.get(CheckFacebookConfigAction.APP_ID);
        FbSsoForm form = loginForm.get();
        return verifyFbToken(form.getInput_token(), accessToken, appId).thenCombineAsync(getEmailFromFb(form.getUserID(), accessToken), (isValid, email) -> {
            if (isValid) {
                try {
                    User user = jpaApi.withTransaction(() -> userService.findByEmail(email));
                    Authenticator.setUser(ctx, user);
                    ObjectNode responseJson = Utils.createAjaxResponse(true);
                    responseJson.put("url", controllers.secured.routes.HomeController.index().url());
                    return ok(responseJson);
                } catch (EnfException e) {
                    ObjectNode json = Utils.createAjaxResponse(false);
                    json.put("message", "No account");
                    json.put("email", email);
                    return ok(json);
                }
            }
            throw new RuntimeException("Invalid Facebook Token");
        }).exceptionally(throwable -> {
            Logger.error("An error occurred using Facebook SSO", throwable);
            ObjectNode json = Utils.createAjaxResponse(false);
            json.put("message", "There was a problem with your login");
            return ok(json);
        });
    }

    private CompletionStage<Boolean> verifyFbToken(String inputToken, String accessToken, String appId) {
        return wsClient.url(FB_TOKEN_ENDPOINT)
                .setQueryParameter("input_token", inputToken)
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

    private CompletionStage<String> getEmailFromFb(String userId, String accessToken) {
        return wsClient.url(FB_USER_EMAIL_ENDPOINT + userId)
                .setQueryParameter("fields", "email")
                .setQueryParameter("access_token", accessToken)
                .get()
                .thenApply(Utils::debugResponse)
                .thenApply(WSResponse::asJson)
                .thenApply(jsonNode -> jsonNode.get("email").asText());
    }

    public static class FbSsoForm {
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
