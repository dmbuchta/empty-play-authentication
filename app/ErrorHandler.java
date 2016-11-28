import play.Configuration;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import utils.Configs;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static play.mvc.Results.*;

/**
 * Created by Dan on 11/28/2016.
 */
@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

    private boolean testingProdErrorHandler;

    @Inject
    public ErrorHandler(Configuration configuration, Environment environment,
                        OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(configuration, environment, sourceMapper, routes);
        testingProdErrorHandler = configuration.getBoolean(Configs.USE_PROD_ERROR_HANDLER, false);
    }

    @Override
    protected CompletionStage<Result> onProdServerError(Http.RequestHeader request, UsefulException exception) {
        return CompletableFuture.completedFuture(internalServerError(views.html.error.render("Something went wrong!",
                "There appears to be something wrong on our end. We're looking into the issue and will fix it ASAP.")));
    }

    @Override
    protected CompletionStage<Result> onDevServerError(Http.RequestHeader request, UsefulException exception) {
        if (testingProdErrorHandler) {
            return onProdServerError(request, exception);
        }
        return super.onDevServerError(request, exception);
    }

    @Override
    protected CompletionStage<Result> onBadRequest(Http.RequestHeader request, String message) {
        return CompletableFuture.completedFuture(forbidden(views.html.error.render(
                "Oops! That was a bad request", message)));
    }

    @Override
    protected CompletionStage<Result> onForbidden(Http.RequestHeader request, String message) {
        return CompletableFuture.completedFuture(forbidden(views.html.error.render(
                "Forbidden", "You do not have permission to view this page.")));
    }

    @Override
    protected CompletionStage<Result> onNotFound(Http.RequestHeader request, String message) {
        return CompletableFuture.completedFuture(notFound(views.html.error.render(
                "Sorry, Page Not Found", "The page you are looking for is not available.")));
    }

    @Override
    protected CompletionStage<Result> onOtherClientError(Http.RequestHeader request, int statusCode, String message) {
        return CompletableFuture.completedFuture(Results.status(statusCode, views.html.error.render(
                "Oops! It appears something went wrong", "We're looking into the issue and will fix it ASAP.")));
    }
}
