package com.example.financePersonal.transactions.dto;

import com.example.financePersonal.transactions.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        LocalDate date,
        String description,
        TransactionType type,
        BigDecimal amount,

        UUID categoryId,
        String categoryName,
        String categoryColorHex,
        String categoryIcon,

        OffsetDateTime createdAt
) {}
