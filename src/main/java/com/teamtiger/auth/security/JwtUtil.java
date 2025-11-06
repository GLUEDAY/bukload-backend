package com.teamtiger.auth.security;

import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

// security/JwtUtil.java
@Component
public class JwtUtil {
    private final Key key;
    private final long accessExp;   // ms
    private final long refreshExp;  // ms

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.accessExpiration}") long accessExp,
                   @Value("${jwt.refreshExpiration}") long refreshExp) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExp = accessExp;
        this.refreshExp = refreshExp;
    }

    public String createAccess(String subject) { return create(subject, accessExp, "access"); }
    public String createRefresh(String subject) { return create(subject, refreshExp, "refresh"); }

    private String create(String subject, long exp, String typ) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)              // subject = loginId
                .claim("typ", typ)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime()+exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getType(String token) {
        Object v = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("typ");
        return v==null? null : v.toString();
    }
}
