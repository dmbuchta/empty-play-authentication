package controllers.security;

import controllers.security.SecurityController;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import play.Configuration;
import play.data.FormFactory;
import play.mvc.Http;
import services.UserService;
import utils.UnitTest;

import static org.mockito.Mockito.when;

/**
 * Created by Dan on 11/19/2016.
 */
public abstract class SecurityControllerTest extends UnitTest {

    @Mock
    protected FormFactory formFactory;
    @Mock
    protected UserService userService;
    @Mock
    protected Configuration configuration;
    @InjectMocks
    protected SecurityController controller;

    @Mock
    protected Http.Context context;
    @Mock
    protected Http.Session session;

    @Override
    public void setUp() {
        super.setUp();
        Http.Context.current.set(context);
        when(context.session()).thenReturn(session);
    }
}
