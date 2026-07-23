package dev.vish.nido.auth;

import dev.vish.nido.common.ConflictException;
import dev.vish.nido.common.NotFoundException;
import dev.vish.nido.security.JwtService;
import dev.vish.nido.user.User;
import dev.vish.nido.user.UserRepository;
import dev.vish.nido.user.UserRole;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be self-registered");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account already exists for this email");
        }
        User user = new User(
                request.name().trim(),
                request.email().trim(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        return response(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(),
                request.password()
        ));
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return response(user);
    }

    private AuthResponse response(User user) {
        JwtService.Token token = jwtService.issue(user);
        return new AuthResponse(
                token.value(),
                token.expiresAt(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
