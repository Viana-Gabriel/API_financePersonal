package com.example.financePersonal.categories.dto;

import com.example.financePersonal.categories.model.CategoryType;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryType type,
        String colorHex,
        String icon,
        boolean archived,
        long transactionsCount
) {}
