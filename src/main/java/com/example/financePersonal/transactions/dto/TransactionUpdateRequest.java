package com.example.financePersonal.transactions.dto;

import com.example.financePersonal.transactions.model.TransactionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionUpdateRequest(
        TransactionType type,
        UUID categoryId,

        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        LocalDate date,

        @Size(max = 140)
        String description,

        String notes
) {}
