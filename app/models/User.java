package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import play.data.validation.Constraints;
import services.AccountService;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Dan on 11/6/2016.
 */
@Entity
@Table(name = "\"user\"")
@NamedQueries({
        @NamedQuery(name = "User.login", query = "select u from User u where u.email = :email and u.encryptedPassword = :password"),
        @NamedQuery(name = "User.findByEmail", query = "select u from User u where u.email = :email")
})
public class User {

    @Id
    @Column(updatable = false)
    @SequenceGenerator(name = "user_id_seq", sequenceName = "user_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq")
    private long id;

    @Constraints.MinLength(3)
    @Constraints.MaxLength(255)
    @Constraints.Email
    @Constraints.Required
    @NotEmpty
    @Column(unique = true, nullable = false, columnDefinition = "varchar(255)")
    private String email;

    @Constraints.MinLength(6)
    @Constraints.MaxLength(255)
    @Transient
    @JsonIgnore
    private String password;

    @Column(name = "password", nullable = false, columnDefinition = "varchar(64)")
    private String encryptedPassword;

    @Column(nullable = false, columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP", updatable = false)
    private Date creationDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public static final User createNewUser(AccountService.NewUserForm newUserFormForm) {
        User user = new User();
        user.setEmail(newUserFormForm.getNewEmail().toLowerCase());
        user.setPassword(newUserFormForm.getNewPassword());
        user.creationDate = new Date();
        return user;
    }
}
