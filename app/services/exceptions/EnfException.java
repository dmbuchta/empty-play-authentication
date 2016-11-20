package services.exceptions;

/**
 * This class was created to distinguish an EntityNotFound Exception,
 * but I didn't want to throw a javax.persistence.EntityNotFoundException
 */
public class EnfException extends RuntimeException {

    public EnfException() {
        super("Entity Not Found!");
    }

    public EnfException(Throwable cause) {
        super(cause);
    }
}
