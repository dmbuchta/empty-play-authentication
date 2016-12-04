package actions;

import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.Configs;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static play.mvc.Http.Status.NOT_IMPLEMENTED;

/**
 * Created by Dan on 11/27/2016.
 */
public class CheckGoogleConfigAction extends Action.Simple {

    private String clientId;

    @Inject
    public CheckGoogleConfigAction(Configuration configuration) {
        super();
        LOGGER.debug("Looking up Google configuration");
        clientId = configuration.getString(Configs.GOOGLE_CLIENT_ID);
        if (StringUtils.isBlank(clientId)) {
            LOGGER.debug("Google Sign in is not configured.");
        } else {
            LOGGER.debug("Everything checks out.");
        }
    }

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        if (StringUtils.isBlank(clientId)) {
            LOGGER.warn("Google configuration is not valid! Please add the appropriate values to conf file.");
            return CompletableFuture.completedFuture(status(NOT_IMPLEMENTED));
        }
        ctx.args.put(CLIENT_ID, clientId);
        return delegate.call(ctx);
    }

    public static final String CLIENT_ID = "CLIENT_ID";
    private static final Logger.ALogger LOGGER = Logger.of(CheckGoogleConfigAction.class);
}
