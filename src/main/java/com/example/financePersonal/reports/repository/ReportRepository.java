package com.example.financePersonal.reports.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ReportRepository extends Repository<com.example.financePersonal.transactions.model.Transaction, UUID> {

    interface MonthYearAggRow {
        Integer getYear();
        Integer getMonth();
        BigDecimal getIncome();
        BigDecimal getExpense();
    }

    interface CategoryYearAggRow {
        UUID getCategoryId();
        String getCategoryName();
        Integer getYear();
        BigDecimal getIncome();
        BigDecimal getExpense();
    }

    interface CategoryYearExpenseRow {
        UUID getCategoryId();
        String getCategoryName();
        Integer getYear();
        BigDecimal getTotal();
    }

    @Query(value = """
        select
          extract(year from t.date)::int  as year,
          extract(month from t.date)::int as month,
          coalesce(sum(case when t.type = 'INCOME'  then t.amount else 0 end), 0) as income,
          coalesce(sum(case when t.type = 'EXPENSE' then t.amount else 0 end), 0) as expense
        from transactions t
        where t.user_id = :userId
          and t.deleted_at is null
          and extract(year from t.date) in (:yearA, :yearB)
        group by extract(year from t.date), extract(month from t.date)
        order by year asc, month asc
        """, nativeQuery = true)
    List<MonthYearAggRow> monthlyAggForTwoYears(
            @Param("userId") UUID userId,
            @Param("yearA") int yearA,
            @Param("yearB") int yearB
    );

    @Query(value = """
        select
          c.id   as categoryId,
          c.name as categoryName,
          extract(year from t.date)::int as year,
          coalesce(sum(case when t.type = 'INCOME'  then t.amount else 0 end), 0) as income,
          coalesce(sum(case when t.type = 'EXPENSE' then t.amount else 0 end), 0) as expense
        from transactions t
        join categories c on c.id = t.category_id
        where t.user_id = :userId
          and t.deleted_at is null
          and extract(year from t.date) in (:yearA, :yearB)
        group by c.id, c.name, extract(year from t.date)
        """, nativeQuery = true)
    List<CategoryYearAggRow> categoryAggForTwoYears(
            @Param("userId") UUID userId,
            @Param("yearA") int yearA,
            @Param("yearB") int yearB
    );

    @Query(value = """
        select
          c.id as categoryId,
          c.name as categoryName,
          extract(year from t.date)::int as year,
          coalesce(sum(t.amount), 0) as total
        from transactions t
        join categories c on c.id = t.category_id
        where t.user_id = :userId
          and t.deleted_at is null
          and t.type = 'EXPENSE'
          and extract(year from t.date) in (:yearA, :yearB)
        group by c.id, c.name, extract(year from t.date)
        """, nativeQuery = true)
    List<CategoryYearExpenseRow> expenseTotalsByCategoryForTwoYears(
            @Param("userId") UUID userId,
            @Param("yearA") int yearA,
            @Param("yearB") int yearB
    );
}
