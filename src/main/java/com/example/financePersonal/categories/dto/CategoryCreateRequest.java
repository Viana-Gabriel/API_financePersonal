package com.example.financePersonal.categories.dto;

import com.example.financePersonal.categories.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull CategoryType type,
        @Size(max = 7) String colorHex,
        @Size(max = 40) String icon
) {}
