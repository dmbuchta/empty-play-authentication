An Empty Play Application...
=================================

This is an empty play application used for quickly getting an app up and running with JPA and Hibernate. 
User authentication is already taken care of for you with a log in screen built with Bootstrap 4. No database interaction is needed.
Additionaly Unit and Application tests have already been written in effort to provide bug free code.

A running example can be found here: https://play-auth.herokuapp.com/

=================================

## Heroku Deployment only needs two steps:
- Generate a secret key (https://www.playframework.com/documentation/2.5.x/ApplicationSecret#generating-an-application-secret).
- Add the key to your enviromental variables with the name "APPLICATION_SECRET" (https://devcenter.heroku.com/articles/config-vars#setting-up-config-vars-for-a-deployed-application).
- push away :)

=================================

## Controllers
- SecurityController.java:
  - Controller used to handle logging in/out and user account creation
- UserRequiredController.java:
  - Abstract controller for all other user required controllers to extend
- HomeController.java:
  - Shows how to handle simple HTTP requests.
  
## Components
- Module.java:
  - Shows how to use Guice to bind all the components needed by your application.
- ApplicationTimer.java:
  - An example of a component that starts when the application starts and stops when the application stops.
- UserRepository.java:
  - A repository for decoupling the JPA dao from the UserService. This is useful for testing the application.

## Filters
- Filters.java:
  - Creates the list of HTTP filters used by your application.
- SecurityFilter.java
  - A simple filter that adds basic security headers to every response. 
- LoggingFilter.java
  - A simple filter that logs requests. Used in dev mode only.
