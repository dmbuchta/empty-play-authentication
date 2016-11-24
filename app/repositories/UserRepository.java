package repositories;

import models.User;
import org.postgresql.util.PSQLException;

import javax.persistence.NoResultException;

/**
 * Created by Dan on 11/19/2016.
 */
public interface UserRepository {

    User findByEmailAndPassword(String email, String encryptedPass) throws NoResultException;

    User findByEmail(String email) throws NoResultException;

    void save(User user, boolean flushImmediately) throws PSQLException;

    void edit(User user);
}
