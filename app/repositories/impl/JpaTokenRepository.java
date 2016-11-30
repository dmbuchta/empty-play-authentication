package repositories.impl;

import models.RefreshToken;
import models.User;
import play.db.jpa.JPAApi;
import repositories.TokenRepository;

import javax.inject.Inject;
import javax.persistence.NoResultException;

/**
 * Created by Dan on 11/29/2016.
 */
public class JpaTokenRepository implements TokenRepository {

    private JPAApi jpaApi;

    @Inject
    public JpaTokenRepository(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    @Override
    public void save(RefreshToken refreshToken) {
        this.jpaApi.em().persist(refreshToken);
    }

    @Override
    public RefreshToken find(String token) {
        return jpaApi.em().find(RefreshToken.class, token);
    }

    @Override
    public RefreshToken findByUserAndClient(User user, String clientId) throws NoResultException {
        return jpaApi.em().createNamedQuery("RefreshToken.findByUserAndClient", RefreshToken.class)
                .setParameter("user", user)
                .setParameter("clientId", clientId)
                .getSingleResult();
    }

    @Override
    public void delete(RefreshToken token) {
        jpaApi.em().remove(token);
    }

    @Override
    public void deleteAndFlush(RefreshToken token) {
        delete(token);
        jpaApi.em().flush();
    }
}
