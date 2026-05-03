package nifreebie.ardodo.service;

import nifreebie.ardodo.dto.request.LoginRequest;
import nifreebie.ardodo.dto.request.RegisterRequest;
import nifreebie.ardodo.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
}
