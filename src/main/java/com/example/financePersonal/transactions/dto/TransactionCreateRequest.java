package com.example.financePersonal.transactions.dto;

import com.example.financePersonal.transactions.model.TransactionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionCreateRequest(
        @NotNull TransactionType type,

        // pode ser null (ex.: categoria removida / "Outros" etc)
        UUID categoryId,

        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @NotNull
        LocalDate date,

        @NotBlank @Size(max = 140)
        String description,

        // opcional
        String notes
) {}
