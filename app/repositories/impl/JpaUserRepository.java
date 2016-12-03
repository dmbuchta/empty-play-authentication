package repositories.impl;

import models.User;
import org.postgresql.util.PSQLException;
import play.db.jpa.JPAApi;
import repositories.UserRepository;

import javax.inject.Inject;
import javax.persistence.NoResultException;

/**
 * Created by Dan on 11/19/2016.
 */
public class JpaUserRepository implements UserRepository {

    protected final JPAApi jpaApi;

    @Inject
    public JpaUserRepository(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    @Override
    public User findByEmailAndPassword(String email, String encryptedPass) throws NoResultException {
        return (User) jpaApi.em().createNamedQuery("User.login")
                .setParameter("email", email)
                .setParameter("password", encryptedPass)
                .getSingleResult();
    }

    @Override
    public User findByEmail(String email) throws NoResultException {
        return (User) jpaApi.em().createNamedQuery("User.findByEmail")
                .setParameter("email", email)
                .getSingleResult();
    }

    @Override
    public void save(User user) {
        jpaApi.em().persist(user);
    }

    @Override
    public void saveAndFlush(User user) throws PSQLException {
        jpaApi.em().persist(user);
        jpaApi.em().flush();
    }

    @Override
    public void edit(User user) {
        jpaApi.em().merge(user);
    }
}
