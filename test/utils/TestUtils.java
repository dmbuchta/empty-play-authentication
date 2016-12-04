package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.filters.csrf.CSRFFilter;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;

import static org.junit.Assert.fail;
import static play.test.Helpers.contentAsString;

/**
 * Created by Dan on 11/23/2016.
 */
public class TestUtils {

    private TestUtils() {
        // Guarantees singleton
    }

    public static ObjectNode parseResult(Result result) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(contentAsString(result), ObjectNode.class);
        } catch (IOException e) {
            fail("Failed Parsing json response");
        }
        return null;
    }

    public static Http.RequestBuilder fakeRequest(String method, String uri) {
        String csrfToken = CSRFFilter.apply$default$3().generateToken();
        return play.test.Helpers.fakeRequest(method, uri + "?csrfToken=" + csrfToken)
                .session(CSRFFilter.apply$default$1().tokenName(), csrfToken);
    }
}
