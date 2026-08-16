package org.likelionhsu.hackathon.auth.oauth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.likelionhsu.hackathon.auth.config.AuthProperties;
import org.likelionhsu.hackathon.auth.config.OAuthProperties;
import org.likelionhsu.hackathon.auth.support.SecureTokenGenerator;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class OAuthStateService {

    private static final String LOGIN_FLOW = "login";
    private static final String ACCOUNT_DELETE_FLOW =
            "account-delete";

    private static final Base64.Encoder BASE64_ENCODER =
            Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER =
            Base64.getUrlDecoder();

    private final SecureTokenGenerator tokenGenerator;
    private final OAuthProperties oauthProperties;
    private final Clock clock;
    private final SecretKeySpec signingKey;

    public OAuthStateService(
            SecureTokenGenerator tokenGenerator,
            AuthProperties authProperties,
            OAuthProperties oauthProperties,
            Clock clock
    ) {
        this.tokenGenerator = tokenGenerator;
        this.oauthProperties = oauthProperties;
        this.clock = clock;
        this.signingKey = new SecretKeySpec(
                authProperties.jwtSecret()
                        .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    public String create() {
        return create(LOGIN_FLOW);
    }

    public String createAccountDeleteReauthentication() {
        return create(ACCOUNT_DELETE_FLOW);
    }

    private String create(String flow) {
        String payload = Instant.now(clock).getEpochSecond()
                + "."
                + flow
                + "."
                + tokenGenerator.generateToken();
        String encodedPayload = BASE64_ENCODER.encodeToString(
                payload.getBytes(StandardCharsets.UTF_8)
        );
        return encodedPayload
                + "."
                + BASE64_ENCODER.encodeToString(sign(encodedPayload));
    }

    public boolean isAccountDeleteReauthentication(String state) {
        String payload = decodePayload(state);
        String[] values = payload.split("\\.", 3);
        return values.length == 3
                && ACCOUNT_DELETE_FLOW.equals(values[1]);
    }

    public void validate(String queryState, String cookieState) {
        if (queryState == null
                || queryState.isBlank()
                || cookieState == null
                || cookieState.isBlank()
                || !MessageDigest.isEqual(
                        queryState.getBytes(StandardCharsets.UTF_8),
                        cookieState.getBytes(StandardCharsets.UTF_8)
                )) {
            throw invalidState();
        }

        String[] parts = queryState.split("\\.", 2);
        if (parts.length != 2) {
            throw invalidState();
        }

        byte[] providedSignature;
        try {
            providedSignature = BASE64_DECODER.decode(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw invalidState();
        }

        if (!MessageDigest.isEqual(
                sign(parts[0]),
                providedSignature
        )) {
            throw invalidState();
        }

        long issuedAtEpochSecond;
        try {
            String payload = decodePayload(queryState);
            issuedAtEpochSecond = Long.parseLong(
                    payload.substring(0, payload.indexOf('.'))
            );
        } catch (RuntimeException exception) {
            throw invalidState();
        }

        Instant now = Instant.now(clock);
        Instant issuedAt = Instant.ofEpochSecond(issuedAtEpochSecond);
        if (issuedAt.isAfter(now)
                || !issuedAt.plus(oauthProperties.stateTtl())
                .isAfter(now)) {
            throw invalidState();
        }
    }

    private String decodePayload(String state) {
        try {
            String encodedPayload = state.split("\\.", 2)[0];
            return new String(
                    BASE64_DECODER.decode(encodedPayload),
                    StandardCharsets.UTF_8
            );
        } catch (RuntimeException exception) {
            throw invalidState();
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "OAuth state 서명에 실패했습니다.",
                    exception
            );
        }
    }

    private BusinessException invalidState() {
        return new BusinessException(ErrorCode.OAUTH_STATE_INVALID);
    }
}
