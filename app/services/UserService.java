package services;

import controllers.secure.SecurityController;
import models.User;
import org.postgresql.util.PSQLException;
import play.Logger;
import repositories.UserRepository;
import services.exceptions.DuplicateEntityException;
import services.exceptions.EnfException;
import utils.Utils;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Dan on 11/19/2016.
 */
public class UserService {

    private final UserRepository repository;

    @Inject
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User login(User user) throws EnfException {
        debug("Logging in User");
        String encryptedPass = encryptString(user.getEmail().toLowerCase() + user.getPassword());
        try {
            return repository.findByEmailAndPassword(user.getEmail(), encryptedPass);
        } catch (NoResultException e) {
            debug("Entity Not Found!");
            throw new EnfException(e);
        }
    }

    public User createNewUser(SecurityController.NewUserForm newUserForm) throws DuplicateEntityException, PSQLException {
        debug("Creating New User");
        User user = User.createNewUser(newUserForm);
        String encryptedPass = encryptString(user.getEmail() + user.getPassword());
        user.setEncryptedPassword(encryptedPass);
        try {
            repository.save(user, true);
        } catch (Exception e) {
            if (Utils.isUniqueKeyViolation(e)) {
                debug("Duplicate Account Found!");
                throw new DuplicateEntityException(e);
            }
            throw e;
        }
        return user;
    }

    private static String encryptString(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-512").digest(value.getBytes("UTF-8"));
            // Must parse out the null character 0x0
            // Reference: https://www.postgresql.org/message-id/1171970019.3101.328.camel%40coppola.muc.ecircle.de
            return new String(hash, "UTF-8").replace("\u0000", "");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void debug(String msg) {
        if (Logger.isDebugEnabled()) {
            Logger.debug(msg);
        }
    }
}
