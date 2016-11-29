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
public class AccountService {

    private final UserRepository repository;

    @Inject
    public AccountService(UserRepository repository) {
        this.repository = repository;
    }

    public User createNewAccount(NewUserForm newUserForm) throws DuplicateEntityException, PSQLException {
        Logger.debug("Creating New User");
        User user = User.createNewUser(newUserForm);
        String encryptedPass = Utils.encryptString(user.getEmail() + user.getPassword());
        user.setEncryptedPassword(encryptedPass);
        try {
            repository.save(user, true);
        } catch (Exception e) {
            if (Utils.isUniqueKeyViolation(e)) {
                Logger.debug("Duplicate Account Found!");
                throw new DuplicateEntityException(e);
            }
            Logger.error("An error occurred creating user", e);
            throw e;
        }
        return user;
    }

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
