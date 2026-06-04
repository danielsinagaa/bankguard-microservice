package com.bankguard.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final String secret;
    private final long expirationSeconds;
    private final Clock clock;

    @Autowired
    public JwtTokenService(
            @Value("${bankguard.security.jwt.secret:bankguard-local-development-secret-change-me}") String secret,
            @Value("${bankguard.security.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        this(secret, expirationSeconds, Clock.systemUTC());
    }

    public JwtTokenService(String secret, long expirationSeconds, Clock clock) {
        this.secret = secret == null || secret.isBlank()
                ? "bankguard-local-development-secret-change-me"
                : secret;
        this.expirationSeconds = expirationSeconds;
        this.clock = clock;
    }

    public String createToken(String username, Collection<String> roles) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":" + quote(username)
                + ",\"roles\":" + rolesJson(roles)
                + ",\"iat\":" + issuedAt.getEpochSecond()
                + ",\"exp\":" + expiresAt.getEpochSecond()
                + "}";

        String encodedHeader = encode(header);
        String encodedPayload = encode(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        return signingInput + "." + sign(signingInput);
    }

    public Optional<JwtClaims> validate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String signingInput = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(signingInput).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            String payload = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            long expiresAt = extractLong(payload, "exp");
            if (Instant.now(clock).getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }

            return Optional.of(new JwtClaims(
                    extractString(payload, "sub"),
                    extractStringArray(payload, "roles"),
                    Instant.ofEpochSecond(extractLong(payload, "iat")),
                    Instant.ofEpochSecond(expiresAt)
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    private static String encode(String value) {
        return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
        }
    }

    private static String rolesJson(Collection<String> roles) {
        return roles == null ? "[]" : roles.stream()
                .map(JwtTokenService::quote)
                .reduce("[", (left, role) -> left.equals("[") ? left + role : left + "," + role)
                + "]";
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String extractString(String json, String claim) {
        Matcher matcher = Pattern.compile("\"" + claim + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing JWT claim: " + claim);
        }
        return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static long extractLong(String json, String claim) {
        Matcher matcher = Pattern.compile("\"" + claim + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing JWT claim: " + claim);
        }
        return Long.parseLong(matcher.group(1));
    }

    private static List<String> extractStringArray(String json, String claim) {
        Matcher arrayMatcher = Pattern.compile("\"" + claim + "\"\\s*:\\s*\\[(.*?)]").matcher(json);
        if (!arrayMatcher.find()) {
            return List.of();
        }

        Matcher valueMatcher = Pattern.compile("\"([^\"]*)\"").matcher(arrayMatcher.group(1));
        List<String> values = new ArrayList<>();
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1));
        }
        return values;
    }
}
