package models;

import org.hibernate.validator.constraints.NotEmpty;
import play.data.validation.Constraints;
import services.caches.AccessTokenCache;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Dan on 11/29/2016.
 */
@Entity
@Table(name = "refresh_token", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "clientId"})
})
@NamedQueries({
        @NamedQuery(name = "RefreshToken.findByUserAndClient", query = "select token from RefreshToken token where token.user = :user and token.clientId = :clientId")
})
public class RefreshToken {

    @Id
    @Constraints.Required
    @NotEmpty
    @Column(updatable = false, columnDefinition = "varchar(255)")
    private String token;

    @Constraints.Required
    @OneToOne
    private User user;

    @Constraints.Required
    @NotEmpty
    @Column(updatable = false, columnDefinition = "varchar(255)")
    private String clientId;


    @Constraints.Required
    @Column(columnDefinition = "TIMESTAMP")
    private Date expirationDate;

    @Constraints.Required
    @NotEmpty
    @Column(updatable = false, columnDefinition = "varchar(255)")
    private String accessToken;

    public RefreshToken() {
        // this is needed for JPA/Jackson
    }

    public RefreshToken(User user, String clientId) {
        this.token = AccessTokenCache.generateRandomToken();
        this.accessToken = AccessTokenCache.generateRandomToken();
        this.user = user;
        this.clientId = clientId;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, AccessTokenCache.refreshTokenDaysUntilExpiration);
        this.expirationDate = calendar.getTime();

    }

    public RefreshToken(RefreshToken refreshToken) {
        this.token = AccessTokenCache.generateRandomToken();
        this.accessToken = AccessTokenCache.generateRandomToken();
        this.user = refreshToken.getUser();
        this.clientId = refreshToken.getClientId();
        this.expirationDate = refreshToken.getExpirationDate();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isExpired() {
        return getExpirationDate().getTime() < new Date().getTime();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

}
