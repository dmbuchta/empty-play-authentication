package repositories.impl;

import models.User;
import org.postgresql.util.PSQLException;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.NoResultException;

/**
 * Created by Dan on 11/28/2016.
 */
public class UnboundJpaUserRepo extends JpaUserRepository {

    @Inject
    public UnboundJpaUserRepo(JPAApi jpaApi) {
        super(jpaApi);
    }

    @Override
    public User findByEmailAndPassword(String email, String encryptedPass) throws NoResultException {
        return jpaApi.withTransaction(() -> super.findByEmailAndPassword(email, encryptedPass));
    }

    @Override
    public User findByEmail(String email) throws NoResultException {
        return jpaApi.withTransaction(() -> super.findByEmail(email));
    }

    @Override
    public void save(User user) {
        jpaApi.withTransaction(() -> super.save(user));
    }


    @Override
    public void saveAndFlush(User user) throws PSQLException {
        // this was an ugly work around to get the PSQL exception to be thrown from the lambda
        try {
            jpaApi.withTransaction(() -> {
                try {
                    super.saveAndFlush(user);
                } catch (PSQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof PSQLException) {
                throw (PSQLException) e.getCause();
            }
            throw e;
        }
    }

    @Override
    public void edit(User user) {
        jpaApi.withTransaction(() -> super.edit(user));
    }
}
