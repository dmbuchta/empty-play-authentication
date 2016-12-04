package services.login.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.name.Named;
import models.User;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public class GoogleLoginService implements LoginService<GoogleLoginService.GoogleLoginForm> {

    private final Set<String> validClientIds;
    private UserRepository repository;
    private WSClient wsClient;
    private final String myClientId;

    @Inject
    public GoogleLoginService(@Named("unbound") UserRepository repository, WSClient wsClient, Configuration configuration) {
        this.repository = repository;
        this.wsClient = wsClient;
        this.myClientId = configuration.getString(Configs.GOOGLE_CLIENT_ID);
        this.validClientIds = new HashSet<>();

        String apiClientsGoogleIds = configuration.getString(Configs.GOOGLE_API_CLIENTS);
        if (!StringUtils.isBlank(myClientId)) {
            validClientIds.add(myClientId);
        }
        if (!StringUtils.isBlank(apiClientsGoogleIds)) {
            validClientIds.addAll(Arrays.asList(apiClientsGoogleIds.split(",")));
        }
        LOGGER.debug("Configured valid client IDs: {}", validClientIds);
    }

    @Override
    public CompletionStage<User> login(Form<GoogleLoginForm> form) {
        LOGGER.debug("Doing a Google User login");
        GoogleLoginForm loginForm = form.get();
        return requestGoogleVerification(loginForm.getId_token())
                .thenApplyAsync((json) -> {
                    if (json.has("aud") && validClientIds.contains(json.get("aud").asText())) {
                        String email = json.get("email").asText();
                        try {
                            return repository.findByEmail(email);
                        } catch (NoResultException e) {
                            LOGGER.debug("Entity Not Found!");
                            throw new EnfException(email);
                        }
                    }
                    LOGGER.warn("Client ID does not match. Someone is doing something very suspicious.");
                    throw new RuntimeException(INVALID_AUD_MESSAGE);
                });
    }

    private CompletionStage<JsonNode> requestGoogleVerification(String token) {
        return wsClient.url(GOOGLE_TOKEN_ENDPOINT)
                .setQueryParameter("id_token", token)
                .get()
                .thenApply(Utils::debugResponse)
                .thenApply(WSResponse::asJson);
    }

    // public to make testing easier
    public static final String GOOGLE_TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v3/tokeninfo";
    public static final String INVALID_AUD_MESSAGE = "Invalid aud from Google";
    private static final Logger.ALogger LOGGER = Logger.of(GoogleLoginService.class);

    public static class GoogleLoginForm {
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
