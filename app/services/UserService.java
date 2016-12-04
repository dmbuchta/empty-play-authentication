package services;

import models.User;
import org.postgresql.util.PSQLException;
import play.Logger;
import play.data.validation.Constraints;
import repositories.UserRepository;
import services.exceptions.DuplicateEntityException;
import utils.Utils;

import javax.inject.Inject;

/**
 * Created by Dan on 11/28/2016.
 */
public class UserService {

    private final UserRepository repository;

    @Inject
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User createNewAccount(NewUserForm newUserForm) throws DuplicateEntityException, PSQLException {
        LOGGER.debug("Creating New User");
        User user = User.createNewUser(newUserForm);
        String encryptedPass = Utils.encryptString(user.getEmail() + user.getPassword());
        user.setEncryptedPassword(encryptedPass);
        try {
            repository.saveAndFlush(user);
        } catch (Exception e) {
            if (Utils.isUniqueKeyViolation(e)) {
                LOGGER.debug("Duplicate Account Found!");
                throw new DuplicateEntityException(e);
            }
            LOGGER.error("An error occurred creating user", e);
            throw e;
        }
        return user;
    }

    private static final Logger.ALogger LOGGER = Logger.of(UserService.class);

    public static class NewUserForm {

        @Constraints.Required
        @Constraints.MinLength(3)
        @Constraints.MaxLength(256)
        @Constraints.Email
        private String newEmail;

        @Constraints.Required
        @Constraints.MinLength(6)
        @Constraints.MaxLength(256)
        private String newPassword;

        @Constraints.Required
        private String confirmPassword;

        public String getNewEmail() {
            return newEmail;
        }

        public void setNewEmail(String newEmail) {
            this.newEmail = newEmail;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
