package com.harshatha.repo_analyzer.service;

import com.harshatha.repo_analyzer.dto.AuthResponse;
import com.harshatha.repo_analyzer.dto.LoginRequest;
import com.harshatha.repo_analyzer.dto.RegisterRequest;
import com.harshatha.repo_analyzer.entity.User;
import com.harshatha.repo_analyzer.repository.UserRepository;
import com.harshatha.repo_analyzer.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder, 
                       JwtService jwtService, 
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        // 1. Check if email is already taken
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered");
        }

        // 2. Create new user entity & encrypt password
        User user = new User();
        user.setEmail(request.getEmail());
        user.setHashPassword(passwordEncoder.encode(request.getPassword())); 

        userRepository.save(user);

        // 3. Generate token immediately upon registration
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate user credentials via Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Fetch user from DB if authentication passed
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Generate and return token
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }
}