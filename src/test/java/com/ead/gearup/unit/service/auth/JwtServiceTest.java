package com.ead.gearup.unit.service.auth;

import com.ead.gearup.fixtures.UserDetailsFixtures;
import com.ead.gearup.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for JwtService
 * Tests JWT token generation, validation, and extraction functionality
 */
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    
    // Test configuration values
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "test-secret-key-for-jwt-testing-must-be-at-least-256-bits-long-for-hs256".getBytes()
    );
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 7200000L; // 2 hours
    private static final long EMAIL_VERIFICATION_EXPIRATION = 300000L; // 5 minutes

    private UserDetails customerUserDetails;
    private UserDetails employeeUserDetails;
    private UserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        
        // Set private fields using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMillis", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenDurationMs", REFRESH_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "emailVerificationTokenDurationMs", EMAIL_VERIFICATION_EXPIRATION);
        
        // Initialize test user details
        customerUserDetails = UserDetailsFixtures.customerUserDetails();
        employeeUserDetails = UserDetailsFixtures.employeeUserDetails();
        adminUserDetails = UserDetailsFixtures.adminUserDetails();
    }

    // ============================================
    // ACCESS TOKEN GENERATION TESTS
    // ============================================

    @Nested
    @DisplayName("Generate Access Token Tests")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("Should generate access token with default claims")
        void generateAccessToken_DefaultClaims_ReturnsValidToken() {
            // Act
            String token = jwtService.generateAccessToken(customerUserDetails);

            // Assert
            assertThat(token).isNotNull().isNotEmpty();
            
            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo("customer@gearup.com");
            assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
            assertThat(claims.get("token_type", String.class)).isEqualTo("access");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("Should generate access token with extra claims")
        void generateAccessToken_WithExtraClaims_IncludesAllClaims() {
            // Arrange
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("requiresPasswordChange", true);
            extraClaims.put("customField", "customValue");

            // Act
            String token = jwtService.generateAccessToken(customerUserDetails, extraClaims);

            // Assert
            Claims claims = parseToken(token);
            assertThat(claims.get("requiresPasswordChange", Boolean.class)).isTrue();
            assertThat(claims.get("customField", String.class)).isEqualTo("customValue");
            assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
            assertThat(claims.get("token_type", String.class)).isEqualTo("access");
        }

        @Test
        @DisplayName("Should remove ROLE_ prefix from role claim")
        void generateAccessToken_WithRolePrefix_RemovesPrefix() {
            // Act
            String token = jwtService.generateAccessToken(employeeUserDetails);

            // Assert
            Claims claims = parseToken(token);
            assertThat(claims.get("role", String.class))
                    .isEqualTo("EMPLOYEE")
                    .doesNotContain("ROLE_");
        }

        @Test
        @DisplayName("Should set correct expiration time for access token")
        void generateAccessToken_ExpirationTime_IsCorrect() {
            // Arrange
            long beforeGeneration = System.currentTimeMillis();
            
            // Act
            String token = jwtService.generateAccessToken(customerUserDetails);
            
            // Assert
            Claims claims = parseToken(token);
            long tokenExpiration = claims.getExpiration().getTime();
            long expectedExpiration = beforeGeneration + ACCESS_TOKEN_EXPIRATION;
            
            // Allow 1 second tolerance for test execution time
            assertThat(tokenExpiration).isBetween(expectedExpiration - 1000, expectedExpiration + 1000);
        }

        @Test
        @DisplayName("Should include JWT type in header")
        void generateAccessToken_HeaderType_IsJWT() {
            // Act
            String token = jwtService.generateAccessToken(customerUserDetails);

            // Assert
            String[] parts = token.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            assertThat(headerJson).contains("\"typ\":\"JWT\"");
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void generateAccessToken_DifferentUsers_GeneratesDifferentTokens() {
            // Act
            String customerToken = jwtService.generateAccessToken(customerUserDetails);
            String employeeToken = jwtService.generateAccessToken(employeeUserDetails);

            // Assert
            assertThat(customerToken).isNotEqualTo(employeeToken);
            
            Claims customerClaims = parseToken(customerToken);
            Claims employeeClaims = parseToken(employeeToken);
            
            assertThat(customerClaims.getSubject()).isEqualTo("customer@gearup.com");
            assertThat(employeeClaims.getSubject()).isEqualTo("employee@gearup.com");
        }

        @Test
        @DisplayName("Should generate different tokens on multiple calls")
        void generateAccessToken_MultipleCalls_GeneratesDifferentTokens() throws InterruptedException {
            // Act
            String token1 = jwtService.generateAccessToken(customerUserDetails);
            Thread.sleep(1100); // Wait over 1 second to ensure different timestamp (JWT uses seconds, not millis)
            String token2 = jwtService.generateAccessToken(customerUserDetails);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should handle admin role correctly")
        void generateAccessToken_AdminUser_IncludesAdminRole() {
            // Act
            String token = jwtService.generateAccessToken(adminUserDetails);

            // Assert
            Claims claims = parseToken(token);
            assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
            assertThat(claims.getSubject()).isEqualTo("admin@gearup.com");
        }
    }

    // ============================================
    // REFRESH TOKEN GENERATION TESTS
    // ============================================

    @Nested
    @DisplayName("Generate Refresh Token Tests")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("Should generate refresh token with correct claims")
        void generateRefreshToken_ValidUser_ReturnsValidToken() {
            // Act
            String token = jwtService.generateRefreshToken(customerUserDetails);

            // Assert
            assertThat(token).isNotNull().isNotEmpty();
            
            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo("customer@gearup.com");
            assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
            assertThat(claims.get("token_type", String.class)).isEqualTo("refresh");
        }

        @Test
        @DisplayName("Should set correct expiration time for refresh token")
        void generateRefreshToken_ExpirationTime_IsLongerThanAccessToken() {
            // Arrange
            long beforeGeneration = System.currentTimeMillis();
            
            // Act
            String token = jwtService.generateRefreshToken(customerUserDetails);
            
            // Assert
            Claims claims = parseToken(token);
            long tokenExpiration = claims.getExpiration().getTime();
            long expectedExpiration = beforeGeneration + REFRESH_TOKEN_EXPIRATION;
            
            assertThat(tokenExpiration).isBetween(expectedExpiration - 1000, expectedExpiration + 1000);
            assertThat(REFRESH_TOKEN_EXPIRATION).isGreaterThan(ACCESS_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("Should remove ROLE_ prefix from role in refresh token")
        void generateRefreshToken_WithRolePrefix_RemovesPrefix() {
            // Act
            String token = jwtService.generateRefreshToken(employeeUserDetails);

            // Assert
            Claims claims = parseToken(token);
            assertThat(claims.get("role", String.class))
                    .isEqualTo("EMPLOYEE")
                    .doesNotContain("ROLE_");
        }

        @Test
        @DisplayName("Should include correct token type")
        void generateRefreshToken_TokenType_IsRefresh() {
            // Act
            String token = jwtService.generateRefreshToken(customerUserDetails);

            // Assert
            Claims claims = parseToken(token);
            assertThat(claims.get("token_type", String.class)).isEqualTo("refresh");
        }
    }

    // ============================================
    // EMAIL VERIFICATION TOKEN GENERATION TESTS
    // ============================================

    @Nested
    @DisplayName("Generate Email Verification Token Tests")
    class GenerateEmailVerificationTokenTests {

        @Test
        @DisplayName("Should generate email verification token")
        void generateEmailVerificationToken_ValidUser_ReturnsValidToken() {
            // Act
            String token = jwtService.generateEmailVerificationToken(customerUserDetails);

            // Assert
            assertThat(token).isNotNull().isNotEmpty();
            
            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo("customer@gearup.com");
            assertThat(claims.get("token_type", String.class)).isEqualTo("email_verification");
        }

        @Test
        @DisplayName("Should have shortest expiration time")
        void generateEmailVerificationToken_ExpirationTime_IsShortest() {
            // Arrange
            long beforeGeneration = System.currentTimeMillis();
            
            // Act
            String token = jwtService.generateEmailVerificationToken(customerUserDetails);
            
            // Assert
            Claims claims = parseToken(token);
            long tokenExpiration = claims.getExpiration().getTime();
            long expectedExpiration = beforeGeneration + EMAIL_VERIFICATION_EXPIRATION;
            
            assertThat(tokenExpiration).isBetween(expectedExpiration - 1000, expectedExpiration + 1000);
            assertThat(EMAIL_VERIFICATION_EXPIRATION)
                    .isLessThan(ACCESS_TOKEN_EXPIRATION)
                    .isLessThan(REFRESH_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("Should not include role claim")
        void generateEmailVerificationToken_NoClaims_DoesNotIncludeRole() {
            // Act
            String token = jwtService.generateEmailVerificationToken(customerUserDetails);

            // Assert
            Claims claims = parseToken(token);
            assertThat(claims.containsKey("role")).isFalse();
        }

        @Test
        @DisplayName("Should include correct token type")
        void generateEmailVerificationToken_TokenType_IsEmailVerification() {
            // Act
            String token = jwtService.generateEmailVerificationToken(customerUserDetails);

            // Assert
            Claims claims = parseToken(token);
            assertThat(claims.get("token_type", String.class)).isEqualTo("email_verification");
        }
    }

    // ============================================
    // TOKEN VALIDATION TESTS
    // ============================================

    @Nested
    @DisplayName("Validate Access Token Tests")
    class ValidateAccessTokenTests {

        @Test
        @DisplayName("Should validate correct access token")
        void validateAccessToken_ValidToken_ReturnsTrue() {
            // Arrange
            String token = jwtService.generateAccessToken(customerUserDetails);

            // Act
            boolean isValid = jwtService.validateAccessToken(token, customerUserDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token with wrong username")
        void validateAccessToken_WrongUsername_ReturnsFalse() {
            // Arrange
            String token = jwtService.generateAccessToken(customerUserDetails);
            UserDetails differentUser = UserDetailsFixtures.employeeUserDetails();

            // Act
            boolean isValid = jwtService.validateAccessToken(token, differentUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void validateAccessToken_ExpiredToken_ReturnsFalse() {
            // Arrange
            String expiredToken = generateExpiredToken(customerUserDetails, "access");

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateAccessToken(expiredToken, customerUserDetails))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Should reject refresh token as access token")
        void validateAccessToken_RefreshToken_ReturnsFalse() {
            // Arrange
            String refreshToken = jwtService.generateRefreshToken(customerUserDetails);

            // Act
            boolean isValid = jwtService.validateAccessToken(refreshToken, customerUserDetails);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject email verification token as access token")
        void validateAccessToken_EmailVerificationToken_ReturnsFalse() {
            // Arrange
            String emailToken = jwtService.generateEmailVerificationToken(customerUserDetails);

            // Act
            boolean isValid = jwtService.validateAccessToken(emailToken, customerUserDetails);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void validateAccessToken_MalformedToken_ThrowsException() {
            // Arrange
            String malformedToken = "invalid.jwt.token";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateAccessToken(malformedToken, customerUserDetails))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Validate Refresh Token Tests")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("Should validate correct refresh token")
        void validateRefreshToken_ValidToken_ReturnsTrue() {
            // Arrange
            String token = jwtService.generateRefreshToken(customerUserDetails);

            // Act
            boolean isValid = jwtService.validateRefreshToken(token, customerUserDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token with wrong username")
        void validateRefreshToken_WrongUsername_ReturnsFalse() {
            // Arrange
            String token = jwtService.generateRefreshToken(customerUserDetails);
            UserDetails differentUser = UserDetailsFixtures.employeeUserDetails();

            // Act
            boolean isValid = jwtService.validateRefreshToken(token, differentUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired refresh token")
        void validateRefreshToken_ExpiredToken_ReturnsFalse() {
            // Arrange
            String expiredToken = generateExpiredToken(customerUserDetails, "refresh");

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateRefreshToken(expiredToken, customerUserDetails))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Should reject access token as refresh token")
        void validateRefreshToken_AccessToken_ReturnsFalse() {
            // Arrange
            String accessToken = jwtService.generateAccessToken(customerUserDetails);

            // Act
            boolean isValid = jwtService.validateRefreshToken(accessToken, customerUserDetails);

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    // ============================================
    // TOKEN EXTRACTION TESTS
    // ============================================

    @Nested
    @DisplayName("Extract Token Claims Tests")
    class ExtractClaimsTests {

        @Test
        @DisplayName("Should extract username from token")
        void extractUsername_ValidToken_ReturnsUsername() {
            // Arrange
            String token = jwtService.generateAccessToken(customerUserDetails);

            // Act
            String username = jwtService.extractUsername(token);

            // Assert
            assertThat(username).isEqualTo("customer@gearup.com");
        }

        @Test
        @DisplayName("Should extract role from token")
        void extractRole_ValidToken_ReturnsRole() {
            // Arrange
            String token = jwtService.generateAccessToken(employeeUserDetails);

            // Act
            String role = jwtService.extractRole(token);

            // Assert
            assertThat(role).isEqualTo("EMPLOYEE");
        }

        @Test
        @DisplayName("Should extract custom claim from token")
        void extractClaim_CustomClaim_ReturnsValue() {
            // Arrange
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("requiresPasswordChange", true);
            String token = jwtService.generateAccessToken(customerUserDetails, extraClaims);

            // Act
            Boolean requiresPasswordChange = jwtService.extractClaim(token, 
                claims -> claims.get("requiresPasswordChange", Boolean.class));

            // Assert
            assertThat(requiresPasswordChange).isTrue();
        }

        @Test
        @DisplayName("Should extract subject claim")
        void extractClaim_Subject_ReturnsSubject() {
            // Arrange
            String token = jwtService.generateAccessToken(adminUserDetails);

            // Act
            String subject = jwtService.extractClaim(token, Claims::getSubject);

            // Assert
            assertThat(subject).isEqualTo("admin@gearup.com");
        }

        @Test
        @DisplayName("Should extract expiration date")
        void extractClaim_Expiration_ReturnsExpirationDate() {
            // Arrange
            String token = jwtService.generateAccessToken(customerUserDetails);

            // Act
            Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

            // Assert
            assertThat(expiration).isNotNull().isAfter(new Date());
        }

        @Test
        @DisplayName("Should extract issued at date")
        void extractClaim_IssuedAt_ReturnsIssuedAtDate() {
            // Arrange
            String token = jwtService.generateAccessToken(customerUserDetails);
            long afterGeneration = System.currentTimeMillis();

            // Act
            Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

            // Assert
            assertThat(issuedAt).isNotNull();
            assertThat(issuedAt.getTime()).isLessThanOrEqualTo(afterGeneration);
            assertThat(issuedAt).isBeforeOrEqualTo(new Date());
        }

        @Test
        @DisplayName("Should throw exception for malformed token")
        void extractUsername_MalformedToken_ThrowsException() {
            // Arrange
            String malformedToken = "not.a.valid.token";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should extract token type")
        void extractClaim_TokenType_ReturnsType() {
            // Arrange
            String accessToken = jwtService.generateAccessToken(customerUserDetails);
            String refreshToken = jwtService.generateRefreshToken(customerUserDetails);

            // Act
            String accessType = jwtService.extractClaim(accessToken, 
                claims -> claims.get("token_type", String.class));
            String refreshType = jwtService.extractClaim(refreshToken, 
                claims -> claims.get("token_type", String.class));

            // Assert
            assertThat(accessType).isEqualTo("access");
            assertThat(refreshType).isEqualTo("refresh");
        }
    }

    // ============================================
    // CONFIGURATION GETTER TESTS
    // ============================================

    @Nested
    @DisplayName("Configuration Getters Tests")
    class ConfigurationGettersTests {

        @Test
        @DisplayName("Should return correct JWT expiration millis")
        void getJwtExpirationMillis_ReturnsCorrectValue() {
            // Act
            long expiration = jwtService.getJwtExpirationMillis();

            // Assert
            assertThat(expiration).isEqualTo(ACCESS_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("Should return correct refresh token duration")
        void getRefreshTokenDurationMs_ReturnsCorrectValue() {
            // Act
            long duration = jwtService.getRefreshTokenDurationMs();

            // Assert
            assertThat(duration).isEqualTo(REFRESH_TOKEN_EXPIRATION);
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Parse a JWT token to extract claims
     */
    private Claims parseToken(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Generate an expired token for testing
     */
    private String generateExpiredToken(UserDetails userDetails, String tokenType) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", tokenType);
        claims.put("role", "CUSTOMER");
        
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago (expired)
                .signWith(key)
                .compact();
    }
}

