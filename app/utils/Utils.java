package utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PSQLException;
import play.Logger;
import play.data.Form;
import play.libs.Json;
import play.libs.ws.WSResponse;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Created by Dan on 11/6/2016.
 */
public class Utils {

    private Utils() {
        //guatantees singleton
    }

    // This is a poor way of doing this but it was a quick and dirty solution
    public static boolean isUniqueKeyViolation(Throwable ex) {
        while (ex != null) {
            if (ex instanceof PSQLException) {
                return ex.getMessage().contains("violates unique constraint");
            } else if (ex instanceof SQLException) {
                // This clause was added because of the H2 db testing only
                return ((SQLException) ex).getErrorCode() == 23505;
            }
            ex = ex.getCause();
        }
        return false;
    }

    public static ObjectNode createAjaxResponse(boolean isSuccess) {
        ObjectNode response = Json.newObject();
        response.put("success", isSuccess);
        return response;
    }

    public static ObjectNode createAjaxResponse(Form form) {
        boolean hasError = form.hasErrors();
        ObjectNode response = createAjaxResponse(!hasError);
        if (hasError) {
            response.set("formErrors", form.errorsAsJson());
        }
        return response;
    }

    public static WSResponse debugResponse(WSResponse response) {
        Logger.debug("HEADERS {}", response.getAllHeaders());
        Logger.debug("Status {}", response.getStatus());
        Logger.debug("Body {}", response.getBody());
        Logger.debug("URI {}", response.getUri());
        return response;
    }

    // Empty block for immediate code testing
    public static void main(String... args) {

        String value = "testemail@playframework.compassword";
        try {
            byte[] hash = MessageDigest.getInstance("SHA-512").digest(value.getBytes("UTF-8"));
            System.out.println(new String(hash, "UTF-8").replace("\u0000", ""));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
