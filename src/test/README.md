# 🧪 Testing Guide for Gear-Up Backend

## 📁 Folder Structure

```
src/test/
├── java/com/ead/gearup/
│   ├── unit/                    # Unit tests (fast, isolated)
│   ├── integration/             # Integration tests (with Spring context)
│   ├── e2e/                     # End-to-end tests
│   ├── fixtures/                # Test data fixtures
│   ├── helpers/                 # Test helper utilities
│   └── config/                  # Test configurations
└── resources/
    ├── application-test.properties
    └── data/                    # Test data files
```

## 🚀 Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=AuthServiceTest
```

### Run Tests with Coverage Report
```bash
mvn clean test
# View report at: target/site/jacoco/index.html
```

### Run Only Unit Tests
```bash
mvn test -Dtest="**/*Test"
```

### Run Only Integration Tests
```bash
mvn test -Dtest="**/*IntegrationTest"
```

## 📊 Current Test Coverage

### Authentication & Security Module (Person 1)
- [x] **AuthServiceTest** - 25 test cases ✅
  - Create User: 8 tests
  - Email Verification: 6 tests
  - Resend Email: 5 tests
  - Login: 5 tests
  - Refresh Token: 4 tests

### Test Files Created
- ✅ `UserFixtures.java` - User test data
- ✅ `DTOFixtures.java` - DTO test data
- ✅ `JwtTestHelper.java` - JWT token utilities
- ✅ `AuthServiceTest.java` - Comprehensive auth tests

## 🔧 Test Dependencies

All dependencies are configured in `pom.xml`:
- JUnit 5 (Jupiter)
- Mockito
- AssertJ
- Spring Boot Test
- Spring Security Test
- H2 Database (in-memory testing)

## 📝 Test Naming Convention

```java
methodName_StateUnderTest_ExpectedBehavior()

// Examples:
createUser_ValidData_ReturnsUserResponse()
verifyEmailToken_ExpiredToken_ReturnsFalse()
resendEmail_WithinCooldown_ThrowsException()
```

## 🎯 Testing Best Practices

### 1. Use AAA Pattern
```java
@Test
void testMethod() {
    // Arrange - Setup test data and mocks
    User user = UserFixtures.verifiedCustomer();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    
    // Act - Execute the method under test
    UserResponseDTO result = authService.getUser(1L);
    
    // Assert - Verify the results
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo("test@gearup.com");
}
```

### 2. Use Fixtures for Test Data
```java
// ❌ Don't create test data inline
User user = new User();
user.setEmail("test@example.com");
user.setName("Test");
// ... many more setters

// ✅ Use fixtures
User user = UserFixtures.verifiedCustomer();
```

### 3. Mock External Dependencies
```java
@Mock
private EmailService emailService;  // Mock external services

@InjectMocks
private AuthService authService;    // Service under test
```

### 4. Use @DisplayName for Clarity
```java
@Test
@DisplayName("Should create user successfully with valid data")
void createUser_ValidData_ReturnsUserResponse() {
    // Test implementation
}
```

### 5. Test Both Happy and Sad Paths
```java
// Happy path
@Test
void createUser_ValidData_ReturnsUserResponse() { ... }

// Sad paths
@Test
void createUser_DuplicateEmail_ThrowsException() { ... }

@Test
void createUser_InvalidEmail_ThrowsException() { ... }
```

## 🐛 Debugging Tests

### Run Single Test in Debug Mode (IntelliJ)
1. Click the green play button next to the test method
2. Select "Debug 'testMethodName()'"

### View Test Output
```bash
mvn test -X  # Debug mode with verbose output
```

### Common Issues & Solutions

#### Issue: Tests fail with "Could not autowire"
**Solution:** You're using `@Autowired` in unit tests. Use `@Mock` and `@InjectMocks` instead.

#### Issue: Database connection errors
**Solution:** Check `application-test.properties` uses H2 in-memory database.

#### Issue: JwtService returns null
**Solution:** Mock the JWT methods:
```java
when(jwtService.generateAccessToken(any())).thenReturn("mock.token");
```

## 📈 Next Steps

### Remaining Tests for Person 1:
1. ✅ AuthServiceTest (COMPLETED)
2. ⏳ JwtServiceTest (Next)
3. ⏳ CustomUserDetailsServiceTest
4. ⏳ EmailVerificationServiceTest
5. ⏳ UserServiceTest
6. ⏳ EmployeeManagementServiceTest
7. ⏳ EmailServiceTest
8. ⏳ RoleBasedAccessServiceTest

### Integration Tests:
- AuthController Integration Tests
- AdminController Integration Tests
- Security Configuration Tests

## 📚 Useful Commands

```bash
# Install dependencies
mvn clean install

# Run tests and skip if failures
mvn test -Dmaven.test.failure.ignore=true

# Run specific test method
mvn test -Dtest=AuthServiceTest#createUser_ValidData_ReturnsUserResponse

# Generate coverage report
mvn jacoco:report

# Clean and run all tests
mvn clean test
```

## 🤝 Collaboration Notes

- **Person 1 (You):** Auth, Security, User Management
- **Person 2:** Business Domain (Customers, Appointments, Projects)

### Shared Files:
- `fixtures/` - Both can add fixtures
- `helpers/` - Both can add helpers
- Don't modify each other's test files

### Git Workflow:
```bash
# Create feature branch
git checkout -b test/auth-security-user-management

# Commit regularly
git add .
git commit -m "test: add AuthService unit tests"

# Push to remote
git push -u origin test/auth-security-user-management
```

## 🎓 Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)

---

**Happy Testing! 🚀**

