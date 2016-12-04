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
public class CheckFacebookConfigAction extends Action.Simple {

    private String accessToken;
    private String appId;

    @Inject
    public CheckFacebookConfigAction(Configuration configuration) {
        super();
        LOGGER.debug("Looking up FB configuration");
        appId = configuration.getString(Configs.FB_APP_ID);
        String appSecret = configuration.getString(Configs.FB_APP_SECRET);
        if (!StringUtils.isBlank(appId) && !StringUtils.isBlank(appSecret)) {
            LOGGER.debug("Everything looks valid... Building FB Access Token.");
            accessToken = appId + "|" + appSecret;
        } else {
            LOGGER.debug("Facebook Sign in is not configured.");
        }
    }

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        if (StringUtils.isAnyBlank(accessToken, appId)) {
            LOGGER.warn("FB configuration is not valid! Please add the appropriate values to conf file.");
            return CompletableFuture.completedFuture(status(NOT_IMPLEMENTED));
        }
        ctx.args.put(ACCESS_TOKEN, accessToken);
        ctx.args.put(APP_ID, appId);
        return delegate.call(ctx);
    }

    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String APP_ID = "APP_ID";
    private static final Logger.ALogger LOGGER = Logger.of(CheckFacebookConfigAction.class);

}
