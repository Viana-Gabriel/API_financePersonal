package com.example.financePersonal.categories.controller;

import com.example.financePersonal.categories.dto.*;
import com.example.financePersonal.categories.model.CategoryType;
import com.example.financePersonal.categories.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(
            @RequestParam(required = false) CategoryType type,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(categoryService.list(type, includeArchived, q));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

}
