package com.example.financePersonal.categories.repository;

import com.example.financePersonal.categories.model.Category;

public interface CategoryWithCount {
    Category getCategory();
    long getTransactionsCount();
}
