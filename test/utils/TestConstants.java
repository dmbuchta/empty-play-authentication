package utils;

/**
 * Created by Dan on 11/29/2016.
 */
public class TestConstants {
    private TestConstants() {
        // Guarantees singleton
    }

    public static final String FAKE_EMAIL = "testemail@playframework.com";
    public static final String FAKE_PASS = "passwd";

    public static final String FAKE_EMAIL_2 = "tester1@playframework.com";


    public static final String WRONG_PASS = "password";
    public static final String INVALID_EMAIL = "invalidEmail";
    public static final String INVALID_PASS = "short";


    public static final long FAKE_USER_ID = 10000;

    // Fake Facebook config values
    public static final String FAKE_APP_ID = "FAKE_APP_ID";
    public static final String FAKE_APP_SECRET = "FAKE_APP_SECRET";
    public static final String FAKE_APP_TOKEN = FAKE_APP_ID + "|" + FAKE_APP_SECRET;
    public static final String FAKE_INPUT_TOKEN = "FAKE_INPUT_TOKEN";
    public static final String WRONG_FAKE_APP_ID = FAKE_APP_ID + "_INCORRECT";

    // Fake Google config values
    public static final String FAKE_CLIENT_ID = "FAKE_CLIENT_ID";
    public static final String FAKE_LOGIN_TOKEN = "FAKE_LOGIN_TOKEN";
    public static final String WRONG_FAKE_CLIENT_ID = FAKE_CLIENT_ID + "_INCORRECT";

    public static final String FAKE_ACCESS_TOKEN = "FAKE_ACCESS_TOKEN";
    public static final String FAKE_REFRESH_TOKEN = "FAKE_REFRESH_TOKEN";

    public static final String FAKE_ACCESS_TOKEN_2 = "FAKE_ACCESS_TOKEN_2";
    public static final String FAKE_REFRESH_TOKEN_2 = "FAKE_REFRESH_TOKEN_2";

    public static final String SESSION_ID_PARAM = "SESSION_ID_PARAM";
    public static final String FAKE_SESSION_ID = "FAKE_SESSION_ID";

}
