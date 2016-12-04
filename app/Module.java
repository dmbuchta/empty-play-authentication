import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import play.http.DefaultHttpErrorHandler;
import repositories.TokenRepository;
import repositories.UserRepository;
import repositories.impl.JpaTokenRepository;
import repositories.impl.JpaUserRepository;
import repositories.impl.UnboundJpaUserRepo;
import services.caches.AccessTokenCache;
import services.caches.SessionCache;
import services.login.LoginService;
import services.login.impl.FacebookLoginService;
import services.login.impl.GoogleLoginService;
import services.login.impl.SimpleLoginService;
import services.oauth.TokenService;
import services.oauth.impl.SimpleTokenService;

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 * <p>
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
public class Module extends AbstractModule {

    @Override
    public void configure() {
        bind(AccessTokenCache.class).asEagerSingleton();
        bind(SessionCache.class).asEagerSingleton();

        bind(DefaultHttpErrorHandler.class).to(ErrorHandler.class);

        bind(UserRepository.class).to(JpaUserRepository.class);
        bind(UserRepository.class).annotatedWith(Names.named("unbound")).to(UnboundJpaUserRepo.class);

        bind(TokenRepository.class).to(JpaTokenRepository.class);


        bind(LoginService.class).to(SimpleLoginService.class);
        bind(LoginService.class).annotatedWith(Names.named("google")).to(GoogleLoginService.class);
        bind(LoginService.class).annotatedWith(Names.named("facebook")).to(FacebookLoginService.class);

        bind(TokenService.class).to(SimpleTokenService.class);

    }

}
