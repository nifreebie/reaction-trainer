package nifreebie.ardodo.dto.response;

import lombok.Builder;

@Builder
public record AuthResponse(String accessToken, String username) {}
