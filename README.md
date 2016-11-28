An Empty Play Application...
=================================

This is an empty play application used for quickly getting an app up and running with JPA and Hibernate. 
User authentication is already taken care of for you with a log in screen built with Bootstrap 4. No database interaction is needed.
Additionaly, Unit and Application tests have been written in effort to provide bug free code.

A running example can be found here: https://play-auth.herokuapp.com/

=================================

## Heroku Deployment:
- Generate a secret key (https://www.playframework.com/documentation/2.5.x/ApplicationSecret#generating-an-application-secret).
- Add the key to your environmental variables with the name "APPLICATION_SECRET" (https://devcenter.heroku.com/articles/config-vars#setting-up-config-vars-for-a-deployed-application).
- If for some reason you don't want to use environmental variable, change the following line in your application.conf file: ``` play.crypto.secret=<YOUR SECRET KEY HERE> ```
- That's it :) now push to your git repo on heroku.

## Google Sign-In:
- To add Google Sign-In capabilities, you need to get a Client ID for your app (https://developers.google.com/identity/sign-in/web/devconsole-project).
- Once you have your Client ID, set it as an environmental variable with the name "SSO_GOOGLE_CLIENT_ID".
- If for some reason you don't want to use an environmental variable, change the following line in your application.conf file: ``` sso.google.client.id=<YOUR CLIENT ID HERE> ```

## Facebook Sign-In:
- To add Facebook Sign-In capabilities, you need to get an App ID and Secret for your app (https://developers.facebook.com/docs/facebook-login/web).
- Once you have your App ID and secret, set it as an environmental variable with the names "SSO_FB_APP_ID" and "SSO_FB_APP_SECRET".
- If for some reason you don't want to use environmental variables, change the following lines in your application.conf file: ``` sso.fb.app.id=<YOUR APP ID HERE>``` and ```sso.fb.app.secret=<YOUR APP SECRET HERE>```
