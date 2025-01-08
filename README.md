# Keycloak 2FA SMS Authenticator
Patched/adjusted to have non-numeric codes "Consonant-vovel-consonant-number-number" with a two second delay between wrong inputs, for security. 
<br/>
And, sending SMS via gatewayapi.com instead of AWS. <br/>
<br/><br/>
Build with maven: **mvn clean package** <br/>
Link in your docker container: **/your-own-path-to-jar-file:/opt/keycloak/providers/dasniko.keycloak-2fa-sms-authenticator.jar** <br/>
Set env in docker-compose: **GATEWAYAPI_KEY=YourApiKey12345** <br/>
<br/>
**You should use conditional access to check for mobile_number if you don't want users without mobile_number to authenticate without SMS**
<br/><br/><br/>


# -- Original Readme -- 


Keycloak Authentication Provider implementation to get a 2nd-factor authentication with a OTP/code/token send via SMS (through AWS SNS).

_Demo purposes only!_

Unfortunately, I don't have a real readme yet.
Blame on me!

But, for now, you can at least read my **blog post** about this autenticator here:  
https://www.n-k.de/2020/12/keycloak-2fa-sms-authentication.html

Or, just watch my **video** about this 2FA SMS SPI:

[![](http://img.youtube.com/vi/GQi19817fFk/maxresdefault.jpg)](http://www.youtube.com/watch?v=GQi19817fFk "")

[![](http://img.youtube.com/vi/FHJ5WOx1es0/maxresdefault.jpg)](http://www.youtube.com/watch?v=FHJ5WOx1es0 "")
