import actions.CheckApiClientAction;
import actions.CheckFacebookConfigAction;
import actions.CheckGoogleConfigAction;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import play.http.DefaultHttpErrorHandler;
import repositories.TokenRepository;
import repositories.UserRepository;
import repositories.impl.JpaTokenRepository;
import repositories.impl.JpaUserRepository;
import repositories.impl.UnboundJpaUserRepo;
import services.AccessTokenCache;
import services.ApplicationTimer;
import services.login.LoginService;
import services.login.impl.FacebookLoginService;
import services.login.impl.GoogleLoginService;
import services.login.impl.SimpleLoginService;
import services.oauth.TokenService;
import services.oauth.impl.SimpleTokenService;

import java.time.Clock;

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
        // Use the system clock as the default implementation of Clock
        bind(Clock.class).toInstance(Clock.systemDefaultZone());
        // Ask Guice to create an instance of ApplicationTimer when the
        // application starts.
        bind(ApplicationTimer.class).asEagerSingleton();
        bind(CheckGoogleConfigAction.class).asEagerSingleton();
        bind(CheckFacebookConfigAction.class).asEagerSingleton();
        bind(CheckApiClientAction.class).asEagerSingleton();
        bind(AccessTokenCache.class).asEagerSingleton();

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
