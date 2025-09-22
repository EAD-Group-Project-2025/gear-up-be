package com.ead.gearup;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.EmailVerificationService;
import com.ead.gearup.service.auth.CustomUserDetailsService;
import com.ead.gearup.service.auth.JwtService;

@SpringBootTest
@ActiveProfiles("test") // uses application-test.properties
class GearupApplicationTests {

	@Mock
	private EmailVerificationService emailVerificationService;

	@Mock
	private JwtService jwtService;

	@Mock
	private CustomUserDetailsService customUserDetailsService;

	@Mock
	private AuthenticationManager authManager;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

}
