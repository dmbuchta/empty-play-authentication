package services.oauth;

import models.RefreshToken;
import models.User;
import services.exceptions.InvalidTokenException;

/**
 * Created by Dan on 11/29/2016.
 */
public interface TokenService {

    RefreshToken createRefreshToken(User user, String clientId);

    RefreshToken updateRefreshToken(String oldRefreshToken, String clientId) throws InvalidTokenException;

    boolean isValidAccessToken(String accessToken);
}
