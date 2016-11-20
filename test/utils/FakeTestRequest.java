package utils;

import play.api.Configuration;
import play.api.Environment;
import play.api.Mode;
import play.api.i18n.DefaultLangs;
import play.api.i18n.DefaultMessagesApi;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Http;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static play.test.Helpers.fakeRequest;

/**
 * Created by Dan on 11/20/2016.
 */
public class FakeTestRequest {

    private Http.RequestBuilder requestBuilder;

    public FakeTestRequest(String method, String uri) {
        this(method, uri, new HashMap<>());
    }

    public FakeTestRequest(String method, String uri, Map<String, String> data) {
        requestBuilder = fakeRequest(method, uri).bodyForm(data);
    }

    public Http.Request build() {
        return requestBuilder.build();
    }
}
