package services.userservice;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.Logger;
import repositories.impl.JPAUserRepository;
import services.UserService;

/**
 * Created by Dan on 11/19/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class UserServiceTest {

    @Mock
    protected JPAUserRepository mockedRepo;
    @InjectMocks
    protected UserService userService;

    @Before
    public void setUp() {
        Logger.debug("Setting Up");
    }

    @After
    public void tearDown() {
        Logger.debug("Tearing Down");
    }
}
