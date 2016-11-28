package controllers;

import play.Logger;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

/**
 * Created by Dan on 11/28/2016.
 */
public class BaseController extends Controller {

    @Inject
    DefaultHttpErrorHandler errorHandler;

    protected Result buildBadRequest(String message) {
        try {
            return buildClientErrorResult(BAD_REQUEST, message);
        } catch (Exception e) {
            Logger.error("An exception occurred getting error page", e);
        }
        return badRequest();
    }

    protected Result buildForbidden(String message) {
        try {
            return buildClientErrorResult(FORBIDDEN, message);
        } catch (Exception e) {
            Logger.error("An exception occurred getting error page", e);
        }
        return badRequest();
    }

    protected Result buildNotFound(String message) {
        try {
            return buildClientErrorResult(NOT_FOUND, message);
        } catch (Exception e) {
            Logger.error("An exception occurred getting error page", e);
        }
        return badRequest();
    }

    protected Result buildInternalServerError(String message) {
        return buildInternalServerError(new RuntimeException(message));
    }

    protected Result buildInternalServerError(Throwable t) {
        try {
            return errorHandler.onServerError(request(), t).toCompletableFuture().get();
        } catch (Exception e) {
            Logger.error("An exception occurred getting error page", e);
        }
        return internalServerError();
    }

    private Result buildClientErrorResult(int statusCode, String message) throws ExecutionException, InterruptedException {
        return errorHandler.onClientError(request(), statusCode, message).toCompletableFuture().get();
    }
}
