package repositories;

import models.RefreshToken;
import models.User;

import javax.persistence.NoResultException;

/**
 * Created by Dan on 11/29/2016.
 */
public interface TokenRepository {

    void save(RefreshToken refreshToken);

    RefreshToken find(String token) throws NoResultException;

    RefreshToken findByUserAndClient(User user, String clientId) throws NoResultException;

    void delete(RefreshToken token);

    void deleteAndFlush(RefreshToken token);
}
