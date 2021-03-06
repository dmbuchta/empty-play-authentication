package actions;

import play.Configuration;
import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.Configs;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static play.mvc.Http.Status.NOT_IMPLEMENTED;

/**
 * Created by Dan on 11/29/2016.
 */
public class CheckApiClientAction extends Action.Simple {

    private final Set<String> clientIds;

    @Inject
    public CheckApiClientAction(Configuration configuration) {
        super();
        LOGGER.debug("Looking up API configuration");
        String clientIdsStr = configuration.getString(Configs.API_CLIENTS, "");
        if (clientIdsStr.isEmpty()) {
            clientIds = new HashSet<>();
        } else {
            clientIds = new HashSet<String>(Arrays.asList(clientIdsStr.split(",")));
        }
        LOGGER.debug("There are {} clients configured.", clientIds.size());
    }

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        if (clientIds.isEmpty()) {
            LOGGER.warn("API configuration is not valid! Please add the appropriate values to conf file.");
            return CompletableFuture.completedFuture(status(NOT_IMPLEMENTED));
        }
        String[] clientIdHeader = ctx.request().headers().get(CLIENT_HEADER);
        if (clientIdHeader == null || clientIdHeader.length != 1) {
            LOGGER.warn("Request header is an invalid");
            return CompletableFuture.completedFuture(badRequest());
        }
        if (!clientIds.contains(clientIdHeader[0])) {
            LOGGER.warn("Client ID is provided but not valid");
            return CompletableFuture.completedFuture(unauthorized());
        }
        // put the client id as a request arg for convenience in later use
        ctx.args.put(CLIENT_REQUEST_ARG, clientIdHeader[0]);
        return delegate.call(ctx);
    }

    public static final String CLIENT_REQUEST_ARG = "CLIENT_REQUEST_ARG";
    private static final String CLIENT_HEADER = "CLIENT_ID";
    private static final Logger.ALogger LOGGER = Logger.of(CheckApiClientAction.class);
}
