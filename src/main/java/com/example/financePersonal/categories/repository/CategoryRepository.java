package com.example.financePersonal.categories.repository;

import com.example.financePersonal.categories.model.Category;
import com.example.financePersonal.categories.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {


    Optional<Category> findByIdAndUser_Id(UUID id, UUID userId);

    boolean existsByUser_IdAndTypeAndNameIgnoreCase(UUID userId, CategoryType type, String name);

    @Query("""
    select c as category,
           count(t) as transactionsCount
    from Category c
    left join Transaction t
           on t.category = c
          and t.deletedAt is null
    where c.user.id = :userId
      and (:type is null or c.type = :type)
      and (:includeArchived = true or c.archived = false)
      and (:q = '' or
           lower(function('unaccent', c.name)) like concat('%', lower(function('unaccent', :q)), '%')
      )
    group by c
    order by c.name asc
""")
    List<CategoryWithCount> findAllWithCount(
            @Param("userId") UUID userId,
            @Param("type") CategoryType type,
            @Param("includeArchived") boolean includeArchived,
            @Param("q") String q
    );


}
