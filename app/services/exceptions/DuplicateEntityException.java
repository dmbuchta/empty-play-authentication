package services.exceptions;

/**
 * Created by Dan on 11/19/2016.
 */
public class DuplicateEntityException extends RuntimeException {

    public DuplicateEntityException() {
        super("Duplicate Entity Exception");
    }

    public DuplicateEntityException(Throwable cause) {
        super(cause);
    }
}
