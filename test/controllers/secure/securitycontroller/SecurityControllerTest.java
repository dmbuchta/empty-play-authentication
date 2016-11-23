package controllers.secure.securitycontroller;

import controllers.secure.SecurityController;
import models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import services.UserService;
import utils.UnitTest;

import java.util.ArrayList;

import static org.mockito.Mockito.when;

/**
 * Created by Dan on 11/19/2016.
 */
public abstract class SecurityControllerTest extends UnitTest {

    @Mock
    protected FormFactory formFactory;
    @Mock
    protected UserService userService;
    @InjectMocks
    protected SecurityController controller;

    @Mock
    protected Http.Context context;
    @Mock
    protected Http.Session session;


}
