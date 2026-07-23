package dev.vish.nido.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.vish.nido.common.ConflictException;
import dev.vish.nido.security.JwtService;
import dev.vish.nido.user.User;
import dev.vish.nido.user.UserRepository;
import dev.vish.nido.user.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registersAHostWithANormalizedEmailAndPasswordHash() {
        RegisterRequest request = new RegisterRequest(
                "Vish Sarraf", "VISH@example.com", "strongpass", UserRole.HOST);
        when(passwordEncoder.encode("strongpass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 7L);
            return user;
        });
        when(jwtService.issue(any(User.class)))
                .thenReturn(new JwtService.Token("token", Instant.parse("2026-08-01T10:00:00Z")));

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("vish@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(response.role()).isEqualTo(UserRole.HOST);
        assertThat(response.token()).isEqualTo("token");
    }

    @Test
    void rejectsAnExistingEmail() {
        RegisterRequest request = new RegisterRequest(
                "Vish", "vish@example.com", "strongpass", UserRole.GUEST);
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An account already exists for this email");
        verify(userRepository, never()).save(any());
    }

    @Test
    void preventsAdministratorSelfRegistration() {
        RegisterRequest request = new RegisterRequest(
                "Admin", "admin@example.com", "strongpass", UserRole.ADMIN);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Administrator accounts cannot be self-registered");
        verify(userRepository, never()).save(any());
    }
}
