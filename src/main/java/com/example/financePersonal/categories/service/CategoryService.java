package com.example.financePersonal.categories.service;

import com.example.financePersonal.categories.dto.*;
import com.example.financePersonal.categories.model.Category;
import com.example.financePersonal.categories.model.CategoryType;
import com.example.financePersonal.categories.repository.CategoryRepository;
import com.example.financePersonal.common.exception.ApiException;
import com.example.financePersonal.security.CurrentUser;
import com.example.financePersonal.users.model.User;
import com.example.financePersonal.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CategoryResponse create(CategoryCreateRequest req) {
        UUID userId = CurrentUser.principal().userId();

        String name = req.name().trim();

        if (categoryRepository.existsByUser_IdAndTypeAndNameIgnoreCase(userId, req.type(), name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category already exists for this type");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        Category category = Category.builder()
                .user(user)
                .name(name)
                .type(req.type())
                .colorHex((req.colorHex() == null || req.colorHex().isBlank()) ? "#C8C8C8" : req.colorHex().trim())
                .icon((req.icon() == null || req.icon().isBlank()) ? null : req.icon().trim())
                .archived(false)
                .build();

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(CategoryType type) {
        UUID userId = CurrentUser.principal().userId();

        List<Category> list = (type == null)
                ? categoryRepository.findAllByUser_IdAndArchivedFalseOrderByNameAsc(userId)
                : categoryRepository.findAllByUser_IdAndArchivedFalseAndTypeOrderByNameAsc(userId, type);

        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse update(UUID categoryId, CategoryUpdateRequest req) {
        UUID userId = CurrentUser.principal().userId();

        Category category = categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));

        if (req.name() != null && !req.name().isBlank()) {
            String newName = req.name().trim();
            if (!newName.equalsIgnoreCase(category.getName())
                    && categoryRepository.existsByUser_IdAndTypeAndNameIgnoreCase(userId, category.getType(), newName)) {
                throw new ApiException(HttpStatus.CONFLICT, "Category already exists for this type");
            }
            category.setName(newName);
        }

        if (req.colorHex() != null && !req.colorHex().isBlank()) {
            category.setColorHex(req.colorHex().trim());
        }

        if (req.icon() != null) {
            category.setIcon(req.icon().isBlank() ? null : req.icon().trim());
        }

        if (req.archived() != null) {
            category.setArchived(req.archived());
        }

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void archive(UUID categoryId) {
        UUID userId = CurrentUser.principal().userId();

        Category category = categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));

        category.setArchived(true);
        categoryRepository.save(category);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getType(),
                c.getColorHex(),
                c.getIcon(),
                c.isArchived()
        );
    }
}
