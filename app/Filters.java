import filters.LoggingFilter;
import filters.SecurityFilter;
import play.Environment;
import play.Mode;
import play.filters.csrf.CSRFFilter;
import play.http.HttpFilters;
import play.mvc.EssentialFilter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class configures filters that run on every request. This
 * class is queried by Play to get a list of filters.
 * <p>
 * Play will automatically use filters from any class called
 * <code>Filters</code> that is placed the root package. You can load filters
 * from a different class by adding a `play.http.filters` setting to
 * the <code>application.conf</code> configuration file.
 */
@Singleton
public class Filters implements HttpFilters {

    private final Environment env;
    private final EssentialFilter securityFilter;
    private final EssentialFilter loggingFilter;
    private final EssentialFilter csrfFilter;

    /**
     * @param env            Basic environment settings for the current application.
     * @param securityFilter A demonstration filter that adds a header to
     */
    @Inject
    public Filters(Environment env, SecurityFilter securityFilter, LoggingFilter loggingFilter, CSRFFilter csrfFilter) {
        this.env = env;
        this.securityFilter = securityFilter;
        this.loggingFilter = loggingFilter;
        this.csrfFilter = csrfFilter.asJava();
    }

    @Override
    public EssentialFilter[] filters() {
        if (env.mode().equals(Mode.DEV)) {
            return new EssentialFilter[]{csrfFilter, securityFilter, loggingFilter};
        } else {
            return new EssentialFilter[]{csrfFilter, securityFilter};
        }
    }



}
