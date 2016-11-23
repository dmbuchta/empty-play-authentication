package services.userservice;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import repositories.impl.JPAUserRepository;
import services.UserService;
import utils.UnitTest;

/**
 * Created by Dan on 11/19/2016.
 */
public abstract class UserServiceTest extends UnitTest {

    @Mock
    protected JPAUserRepository mockedRepo;
    @InjectMocks
    protected UserService userService;
}
