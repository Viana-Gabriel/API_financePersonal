package com.example.financePersonal.categories.repository;

import com.example.financePersonal.categories.model.Category;
import com.example.financePersonal.categories.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByUser_IdAndArchivedFalseOrderByNameAsc(UUID userId);

    List<Category> findAllByUser_IdAndArchivedFalseAndTypeOrderByNameAsc(UUID userId, CategoryType type);

    Optional<Category> findByIdAndUser_Id(UUID id, UUID userId);

    boolean existsByUser_IdAndTypeAndNameIgnoreCase(UUID userId, CategoryType type, String name);
}
