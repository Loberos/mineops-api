package com.mineops.mineopsapi.iam.infrastructure.tokens.jwt.services;

import com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.configuration.SecurityProperties;
import com.mineops.mineopsapi.iam.infrastructure.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
public class TokenServiceImpl implements BearerTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecurityProperties.Jwt settings;
    private final SecretKey signingKey;

    public TokenServiceImpl(SecurityProperties securityProperties) {
        this.settings = securityProperties.jwt();
        this.signingKey = Keys.hmacShaKeyFor(settings.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(String email) {
        var issuedAt = Instant.now();
        var expiresAt = issuedAt.plus(Duration.ofHours(settings.expirationHours()));
        return Jwts.builder()
                .subject(email)
                .issuer(settings.issuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String getEmailFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(settings.issuer())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException exception) {
            LOGGER.warn("Se rechazó un token con firma inválida");
        } catch (ExpiredJwtException exception) {
            LOGGER.debug("Se rechazó un token vencido");
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException exception) {
            LOGGER.warn("Se rechazó un token mal formado: {}", exception.getMessage());
        }
        return false;
    }

    @Override
    public String getBearerTokenFrom(HttpServletRequest request) {
        var header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        var claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
