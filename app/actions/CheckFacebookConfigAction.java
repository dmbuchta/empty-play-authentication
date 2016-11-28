package actions;

import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.Configs;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static play.mvc.Http.Status.NOT_IMPLEMENTED;

/**
 * Created by Dan on 11/27/2016.
 */
@Singleton
public class CheckFacebookConfigAction extends Action.Simple {

    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String APP_ID = "APP_ID";

    private String accessToken;
    private String appId;

    @Inject
    public CheckFacebookConfigAction(Configuration configuration) {
        super();
        Logger.debug("Looking up FB configuration");
        appId = configuration.getString(Configs.FB_APP_ID);
        String appSecret = configuration.getString(Configs.FB_APP_SECRET);
        if (!StringUtils.isBlank(appId) && !StringUtils.isBlank(appSecret)) {
            Logger.debug("Everything looks valid... Building Access Token.");
            accessToken = appId + "|" + appSecret;
        }
    }

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        if (StringUtils.isAnyBlank(accessToken, appId)) {
            Logger.warn("FB configuration is not valid! Please add the appropriate values to conf file.");
            return CompletableFuture.completedFuture(status(NOT_IMPLEMENTED));
        }
        ctx.args.put(ACCESS_TOKEN, accessToken);
        ctx.args.put(APP_ID, appId);
        return delegate.call(ctx);
    }
}
