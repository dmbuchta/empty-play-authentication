package utils;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import play.Logger;

/**
 * Created by Dan on 11/23/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class UnitTest {

    @Before
    public void setUp() {
        Logger.debug("Setting Up");
    }

    @After
    public void tearDown() {
        Logger.debug("Tearing Down");
    }
}
