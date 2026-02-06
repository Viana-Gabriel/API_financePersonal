package com.example.financePersonal.reports.dto;

import java.util.List;
import java.util.Map;

public record ComparativeReportResponse(
        Filters filters,
        Cards cards,
        Chart chart,
        Insights insights,
        List<TopChangeItem> topChanges,
        CategoriesTable categoriesTable
) {
    public record Filters(int yearA, int yearB, ReportMetric metric) {}

    public record CardItem(Number a, Number b, Number diff, Number percent) {}
    public record Cards(CardItem income, CardItem expense, CardItem balance) {}

    public record Chart(List<String> labels, List<Series> series) {
        public record Series(String name, List<java.math.BigDecimal> data) {}
    }

    public record Insights(String highlight, String peakMonthText, String yearBalanceText) {}

    public record TopChangeItem(int rank, String categoryId, String categoryName, Number diff, Number percent) {}



    public record CategoriesTable(List<Row> rows, Map<String, String> sort) {
        public record Row(String categoryId, String category, Number a, Number b, Number diff, Number percent) {}
    }
}
