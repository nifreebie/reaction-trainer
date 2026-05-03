package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.domain.Player;
import nifreebie.ardodo.dto.request.LoginRequest;
import nifreebie.ardodo.dto.request.RegisterRequest;
import nifreebie.ardodo.dto.response.AuthResponse;
import nifreebie.ardodo.repository.PlayerRepository;
import nifreebie.ardodo.service.AuthService;
import nifreebie.ardodo.util.DuplicateUsernameException;
import nifreebie.ardodo.util.JwtUtil;
import nifreebie.ardodo.util.NotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (playerRepository.existsByName(request.name())) {
            throw new DuplicateUsernameException("Username already exists");
        }

        Player player = Player.builder()
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        playerRepository.save(player);
        return buildAuthResponse(player.getName());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        Player player = playerRepository.findByName(request.username()).orElseThrow(NotFoundException::new);
        return buildAuthResponse(player.getName());
    }

    private AuthResponse buildAuthResponse(String username) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(username))
                .username(username)
                .build();
    }
}
