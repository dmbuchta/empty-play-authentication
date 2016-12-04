package services.login.impl;

import models.User;
import play.Logger;
import play.data.Form;
import repositories.UserRepository;
import services.exceptions.EnfException;
import services.login.LoginService;
import utils.Utils;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Dan on 11/28/2016.
 */
public class SimpleLoginService implements LoginService<User> {

    private UserRepository repository;

    @Inject
    public SimpleLoginService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletionStage<User> login(Form<User> form) {
        LOGGER.debug("Doing a simple User login");
        User user = form.get();
        String encryptedPass = Utils.encryptString(user.getEmail().toLowerCase() + user.getPassword());
        try {
            return CompletableFuture.completedFuture(repository.findByEmailAndPassword(user.getEmail().toLowerCase(), encryptedPass));
        } catch (NoResultException e) {
            LOGGER.debug("Entity Not Found!");
            throw new EnfException(user.getEmail());
        }
    }

    private static final Logger.ALogger LOGGER = Logger.of(SimpleLoginService.class);
}
