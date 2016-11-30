package services.exceptions;

/**
 * Created by Dan on 11/29/2016.
 */
public class InvalidTokenException extends Exception {

    public InvalidTokenException(String message) {
        super(message);
    }
}
