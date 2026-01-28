package com.example.financePersonal.categories.dto;

import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @Size(max = 80) String name,
        @Size(max = 7) String colorHex,
        @Size(max = 40) String icon,
        Boolean archived
) {}