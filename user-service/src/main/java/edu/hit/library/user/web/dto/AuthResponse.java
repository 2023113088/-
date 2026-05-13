package edu.hit.library.user.web.dto;

public record AuthResponse(String token, Long userId, String username, String role) {}
