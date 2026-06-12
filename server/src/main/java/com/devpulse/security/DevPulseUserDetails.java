package com.devpulse.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class DevPulseUserDetails {
    private final UUID userId;
    private final UUID teamId;
    private final String username;
}