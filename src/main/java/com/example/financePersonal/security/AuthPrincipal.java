package com.example.financePersonal.security;

import java.util.UUID;

public record AuthPrincipal(UUID userId, String email) {}