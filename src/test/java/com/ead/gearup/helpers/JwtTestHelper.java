package com.ead.gearup.helpers;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for generating and manipulating JWT tokens in tests
 */
public class JwtTestHelper {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-in-tests-must-be-long-enough";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

    /**
     * Generates a valid access token for testing
     */
    public static String generateValidAccessToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "access");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates an expired access token for testing
     */
    public static String generateExpiredAccessToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "access");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a valid refresh token for testing
     */
    public static String generateValidRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "refresh");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 604800000)) // 7 days
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a valid email verification token for testing
     */
    public static String generateValidEmailVerificationToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "email_verification");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 300000)) // 5 minutes
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates an expired email verification token for testing
     */
    public static String generateExpiredEmailVerificationToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "email_verification");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 600000)) // 10 min ago
                .setExpiration(new Date(System.currentTimeMillis() - 300000)) // 5 min ago
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a token with wrong type
     */
    public static String generateWrongTypeToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "wrong_type");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts claims from a token
     */
    public static Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

