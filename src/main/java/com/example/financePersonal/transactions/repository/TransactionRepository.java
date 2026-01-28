package com.example.financePersonal.transactions.repository;

import com.example.financePersonal.transactions.dto.TransactionResponse;
import com.example.financePersonal.transactions.model.Transaction;
import com.example.financePersonal.transactions.model.TransactionType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUser_Id(UUID id, UUID userId);

    @Query("""
            select t
            from Transaction t
            left join fetch t.category c
            where t.user.id = :userId
              and t.deletedAt is null
              and extract(year from t.date) = :year
              and (:type is null or t.type = :type)
              and (:categoryId is null or c.id = :categoryId)
              and (:q is null or :q = '' or lower(t.description) like lower(concat('%', :q, '%')))
            order by t.date desc, t.createdAt desc
            """)
    Page<Transaction> searchPage(
            @Param("userId") UUID userId,
            @Param("year") Integer year,
            @Param("type") TransactionType type,
            @Param("categoryId") UUID categoryId,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
                select new com.example.financePersonal.transactions.dto.TransactionResponse(
                    t.id, t.date, t.description, t.type, t.amount,
                    c.id, c.name, c.colorHex, c.icon,
                    t.createdAt
                )
                from Transaction t
                left join t.category c
                where t.user.id = :userId
                  and t.deletedAt is null
                  and t.id = :id
            """)
    Optional<TransactionResponse> findDetails(
            @Param("userId") UUID userId,
            @Param("id") UUID id
    );
}
