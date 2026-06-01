package io.sertaoBit.odontocore.crm.config.security;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.sertaoBit.odontocore.crm.modules.identity.security.MainUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;


    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(this.secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public String generateToken(MainUser mainUser) {
        return Jwts.builder()
                .subject(mainUser.getUsername())
                .claim("id", mainUser.getId())
                .claim("role", mainUser.getRole())
                .claim("sector", mainUser.getSector())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }


    private boolean isExpired(String token) {
        Date expired = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expired.before(new Date());
    }

    public boolean isValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isExpired(token);
    }

    public String extractUserNameIgnoringExpiration(String token) throws JwtException {
            try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException ex) {
                return ex.getClaims().getSubject();
            }
            catch (JwtException e)
                {
                throw new JwtException(e.getMessage());
                }
    }
}
