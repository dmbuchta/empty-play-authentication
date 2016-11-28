package controllers.security.sso;

import actions.CheckGoogleConfigAction;
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
 * Created by Dan on 11/23/2016.
 */
public class GoogleSsoController extends Controller {

    private static final String GOOGLE_TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v3/tokeninfo";

    private JPAApi jpaApi;
    private FormFactory formFactory;
    private UserService userService;
    private WSClient wsClient;

    @Inject
    public GoogleSsoController(JPAApi jpaApi, FormFactory formFactory, UserService userService, WSClient wsClient) {
        this.jpaApi = jpaApi;
        this.formFactory = formFactory;
        this.userService = userService;
        this.wsClient = wsClient;
    }

    @Transactional(readOnly = true)
    @With(CheckGoogleConfigAction.class)
    public CompletionStage<Result> login() {
        Form<GoogleSsoForm> loginForm = formFactory.form(GoogleSsoForm.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return CompletableFuture.completedFuture(ok(Utils.createAjaxResponse(loginForm)));
        }
        final Http.Context ctx = ctx();
        final String clientId = (String) ctx.args.get(CheckGoogleConfigAction.CLIENT_ID);
        CompletionStage<Result> result = wsClient.url(GOOGLE_TOKEN_ENDPOINT)
                .setQueryParameter("id_token", loginForm.get().getId_token())
                .get()
                .thenApply(Utils::debugResponse)
                .thenApply(WSResponse::asJson)
                .thenApplyAsync((json) -> {
                    if (json.has("aud") && json.get("aud").asText("").equals(clientId)) {
                        String email = json.get("email").asText();
                        User user = jpaApi.withTransaction(() -> userService.findByEmail(email));
                        Authenticator.setUser(ctx, user);
                        ObjectNode responseJson = Utils.createAjaxResponse(true);
                        responseJson.put("url", controllers.secured.routes.HomeController.index().url());
                        return ok(responseJson);
                    }
                    throw new RuntimeException("Invalid aud from Google");
                }).exceptionally(throwable -> {
                    ObjectNode json = Utils.createAjaxResponse(false);
                    if (throwable.getCause() instanceof EnfException) {
                        json.put("message", "No account");
                    } else {
                        Logger.error("An error occurred using Google SSO", throwable);
                        json.put("message", "There was a problem with your login");
                    }
                    return ok(json);
                });
        return result;
    }

    public static class GoogleSsoForm {
        @NotEmpty
        @Constraints.Required
        private String id_token;

        public String getId_token() {
            return id_token;
        }

        public void setId_token(String id_token) {
            this.id_token = id_token;
        }
    }
}
