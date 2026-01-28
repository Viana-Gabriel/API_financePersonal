package com.example.financePersonal.transactions.controller;

import com.example.financePersonal.transactions.dto.*;
import com.example.financePersonal.transactions.model.TransactionType;
import com.example.financePersonal.transactions.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@RequestBody @Valid TransactionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(req));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(transactionService.list(year, type, categoryId, q, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.get(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable UUID id,
                                                      @RequestBody TransactionUpdateRequest req) {
        return ResponseEntity.ok(transactionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
