package dasniko.keycloak.authenticator;

import dasniko.keycloak.authenticator.gateway.SmsServiceFactory;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

import java.util.Locale;
import java.util.Random;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class SmsAuthenticator implements Authenticator {

    private static final String MOBILE_NUMBER_FIELD = "mobile_number";
    private static final String TPL_CODE = "login-sms.ftl";

    private static final String CONSONANTS = "bcdfghjklmnpqrstvwxz";
    private static final String VOWELS = "aeiouy";
    private static final String DIGITS = "0123456789";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        KeycloakSession session = context.getSession();
        UserModel user = context.getUser();

        String mobileNumber = user.getFirstAttribute(MOBILE_NUMBER_FIELD);

        // Length and TTL from config
        int length = Integer.parseInt(config.getConfig().get(SmsConstants.CODE_LENGTH));
        int ttl = Integer.parseInt(config.getConfig().get(SmsConstants.CODE_TTL));

        // Generate the custom code
        String code = generateCustomCode();

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(SmsConstants.CODE, code);
        authSession.setAuthNote(SmsConstants.CODE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));

        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = session.getContext().resolveLocale(user);
            String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
            String smsText = String.format(smsAuthText, code, Math.floorDiv(ttl, 60));

            SmsServiceFactory.get(config.getConfig()).send(mobileNumber, smsText);

            context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
        } catch (Exception e) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                context.form().setError("smsAuthSmsNotSent", e.getMessage())
                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst(SmsConstants.CODE);

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String code = authSession.getAuthNote(SmsConstants.CODE);
        String ttl = authSession.getAuthNote(SmsConstants.CODE_TTL);

        if (code == null || ttl == null) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        boolean isValid = enteredCode.equals(code);
        if (isValid) {
            if (Long.parseLong(ttl) < System.currentTimeMillis()) {
                // expired
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                    context.form().setError("smsAuthCodeExpired").createErrorPage(Response.Status.BAD_REQUEST));
            } else {
                // valid
                context.success();
            }
        } else {
            // invalid
            AuthenticationExecutionModel execution = context.getExecution();
            if (execution.isRequired()) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form().setAttribute("realm", context.getRealm())
                        .setError("smsAuthCodeInvalid").createForm(TPL_CODE));
		try {
                // Introduce a delay of 2 seconds (2000 milliseconds)
                Thread.sleep(2000);
            	} catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            	}
            } else if (execution.isConditional() || execution.isAlternative()) {
                context.attempted();
            }
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getFirstAttribute(MOBILE_NUMBER_FIELD) != null;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // this will only work if you have the required action from here configured:
        // https://github.com/dasniko/keycloak-extensions-demo/tree/main/requiredaction
        user.addRequiredAction("mobile-number-ra");
    }

    @Override
    public void close() {
    }

    /**
     * Generates a custom code in the format: consonant + vowel + consonant + digit + digit
     */
    private String generateCustomCode() {
        Random rand = new Random();

        // Generate consonant
        char consonant = CONSONANTS.charAt(rand.nextInt(CONSONANTS.length()));

        // Generate vowel
        char vowel = VOWELS.charAt(rand.nextInt(VOWELS.length()));

        // Generate digit
        char digit = DIGITS.charAt(rand.nextInt(DIGITS.length()));

        // Build the string in the required pattern
        StringBuilder sb = new StringBuilder();
        sb.append(consonant);  // First consonant
        sb.append(vowel);      // Vowel
        sb.append(consonant);  // Same consonant
        sb.append(digit);      // First digit
        sb.append(digit);      // Same digit

        return sb.toString();
    }
}
