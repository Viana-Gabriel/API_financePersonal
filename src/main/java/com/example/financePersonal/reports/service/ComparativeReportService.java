package com.example.financePersonal.reports.service;

import com.example.financePersonal.common.exception.ApiException;
import com.example.financePersonal.reports.dto.ComparativeReportResponse;
import com.example.financePersonal.reports.dto.ReportMetric;
import com.example.financePersonal.reports.repository.ReportRepository;
import com.example.financePersonal.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ComparativeReportService {

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public ComparativeReportResponse build(int yearA, int yearB, ReportMetric metricForChart) {
        UUID userId = CurrentUser.principal().userId();

        if (yearA <= 0 || yearB <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "years are required");
        }
        if (yearA == yearB) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "yearA and yearB must be different");
        }

        var filters = new ComparativeReportResponse.Filters(yearA, yearB, metricForChart);

        // 1) dados base do gráfico e cards (mensal por 2 anos)
        var monthRows = reportRepository.monthlyAggForTwoYears(userId, yearA, yearB);

        // 2) dados base da tabela (por categoria por 2 anos)
        // (não depende de metric!)
        var catRows = reportRepository.categoryAggForTwoYears(userId, yearA, yearB);

        // ===== Cards (sempre visão geral) =====
        var cards = buildCards(monthRows, yearA, yearB);

        // ===== Chart (depende do metric) =====
        var chart = buildChart(monthRows, yearA, yearB, metricForChart);

        // ===== Tabela por Categoria (sempre visão geral) =====
        var categoriesTable = buildCategoryTableOverall(catRows, yearA, yearB);

        // ===== Top mudanças (sempre visão geral) =====
        var topChanges = buildTopChanges(categoriesTable);

        // ===== Insights (sempre visão geral) =====
        var insights = buildInsights(monthRows, topChanges, yearA, yearB);

        return new ComparativeReportResponse(
                filters,
                cards,
                chart,
                insights,
                topChanges,
                categoriesTable
        );
    }

    // ===================== Cards =====================

    private ComparativeReportResponse.Cards buildCards(
            List<ReportRepository.MonthYearAggRow> rows,
            int yearA, int yearB
    ) {
        BigDecimal incomeA = BigDecimal.ZERO;
        BigDecimal incomeB = BigDecimal.ZERO;
        BigDecimal expenseA = BigDecimal.ZERO;
        BigDecimal expenseB = BigDecimal.ZERO;

        for (var r : rows) {
            if (Objects.equals(r.getYear(), yearA)) {
                incomeA = incomeA.add(nvl(r.getIncome()));
                expenseA = expenseA.add(nvl(r.getExpense()));
            } else if (Objects.equals(r.getYear(), yearB)) {
                incomeB = incomeB.add(nvl(r.getIncome()));
                expenseB = expenseB.add(nvl(r.getExpense()));
            }
        }

        BigDecimal balanceA = incomeA.subtract(expenseA);
        BigDecimal balanceB = incomeB.subtract(expenseB);

        var income = toCardItem(incomeA, incomeB);
        var expense = toCardItem(expenseA, expenseB);
        var balance = toCardItem(balanceA, balanceB);

        return new ComparativeReportResponse.Cards(income, expense, balance);
    }

    private ComparativeReportResponse.CardItem toCardItem(BigDecimal a, BigDecimal b) {
        BigDecimal diff = b.subtract(a);
        BigDecimal percent = percent(a, b);
        return new ComparativeReportResponse.CardItem(a, b, diff, percent);
    }

    // ===================== Chart =====================

    private ComparativeReportResponse.Chart buildChart(
            List<ReportRepository.MonthYearAggRow> rows,
            int yearA, int yearB,
            ReportMetric metric
    ) {
        var labels = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> Month.of(m).name().substring(0, 3).toLowerCase())
                .toList();

        BigDecimal[] a = new BigDecimal[12];
        BigDecimal[] b = new BigDecimal[12];
        Arrays.fill(a, BigDecimal.ZERO);
        Arrays.fill(b, BigDecimal.ZERO);

        for (var r : rows) {
            int monthIdx = r.getMonth() - 1;

            BigDecimal income = nvl(r.getIncome());
            BigDecimal expense = nvl(r.getExpense());
            BigDecimal value = switch (metric) {
                case INCOME -> income;
                case EXPENSE -> expense;
                case BALANCE -> income.subtract(expense);
            };

            if (Objects.equals(r.getYear(), yearA)) a[monthIdx] = a[monthIdx].add(value);
            if (Objects.equals(r.getYear(), yearB)) b[monthIdx] = b[monthIdx].add(value);
        }

        var series = List.of(
                new ComparativeReportResponse.Chart.Series(String.valueOf(yearA), Arrays.stream(a).toList()),
                new ComparativeReportResponse.Chart.Series(String.valueOf(yearB), Arrays.stream(b).toList())
        );

        return new ComparativeReportResponse.Chart(labels, series);
    }

    // ===================== Tabela por Categoria (visão geral) =====================

    private ComparativeReportResponse.CategoriesTable buildCategoryTableOverall(
            List<ReportRepository.CategoryYearAggRow> rows,
            int yearA, int yearB
    ) {
        record Key(UUID categoryId, String categoryName) {}

        Map<Key, BigDecimal> aMap = new HashMap<>();
        Map<Key, BigDecimal> bMap = new HashMap<>();

        for (var r : rows) {
            var key = new Key(r.getCategoryId(), r.getCategoryName());

            // visão geral por categoria: total = income + expense
            BigDecimal total = nvl(r.getIncome()).add(nvl(r.getExpense()));

            if (Objects.equals(r.getYear(), yearA)) aMap.merge(key, total, BigDecimal::add);
            if (Objects.equals(r.getYear(), yearB)) bMap.merge(key, total, BigDecimal::add);
        }

        var keys = new HashSet<Key>();
        keys.addAll(aMap.keySet());
        keys.addAll(bMap.keySet());

        var tableRows = keys.stream()
                .map(k -> {
                    BigDecimal a = aMap.getOrDefault(k, BigDecimal.ZERO);
                    BigDecimal b = bMap.getOrDefault(k, BigDecimal.ZERO);
                    BigDecimal diff = b.subtract(a);
                    BigDecimal pct = percent(a, b);

                    return new ComparativeReportResponse.CategoriesTable.Row(
                            k.categoryId().toString(),
                            k.categoryName(),
                            a, b,
                            diff, pct
                    );
                })
                // ordena por abs(diff) desc (padrão bom pro “top mudanças” e tabela)
                .sorted((x, y) -> toBig(y.diff()).abs().compareTo(toBig(x.diff()).abs()))
                .toList();

        // se você não usa sort ainda, pode manter fixo
        Map<String, String> sort = Map.of("by", "diffAbs", "direction", "desc");

        return new ComparativeReportResponse.CategoriesTable(tableRows, sort);
    }

    // ===================== Top mudanças (usa a tabela) =====================

    private List<ComparativeReportResponse.TopChangeItem> buildTopChanges(
            ComparativeReportResponse.CategoriesTable table
    ) {
        var rows = table.rows();
        int max = Math.min(3, rows.size());

        return IntStream.range(0, max)
                .mapToObj(i -> {
                    var r = rows.get(i);
                    return new ComparativeReportResponse.TopChangeItem(
                            i + 1,
                            r.categoryId(),
                            r.category(),
                            r.diff(),
                            r.percent()
                    );
                })
                .toList();
    }

    // ===================== Insights (simples por enquanto) =====================

    private ComparativeReportResponse.Insights buildInsights(
            List<ReportRepository.MonthYearAggRow> monthRows,
            List<ComparativeReportResponse.TopChangeItem> topChanges,
            int yearA, int yearB
    ) {
        String highlight = topChanges.isEmpty()
                ? "Sem mudanças relevantes"
                : String.format(
                "%s %s (%d vs %d)",
                topChanges.get(0).categoryName(),
                (toBig(topChanges.get(0).diff()).signum() >= 0 ? "subiu" : "caiu"),
                yearB, yearA
        );

        // maior gasto em yearB (pela soma mensal de despesas)
        BigDecimal best = BigDecimal.valueOf(-1);
        int bestMonth = 1;

        BigDecimal[] expenseByMonthB = new BigDecimal[12];
        Arrays.fill(expenseByMonthB, BigDecimal.ZERO);

        for (var r : monthRows) {
            if (Objects.equals(r.getYear(), yearB)) {
                expenseByMonthB[r.getMonth() - 1] = expenseByMonthB[r.getMonth() - 1].add(nvl(r.getExpense()));
            }
        }
        for (int i = 0; i < 12; i++) {
            if (expenseByMonthB[i].compareTo(best) > 0) {
                best = expenseByMonthB[i];
                bestMonth = i + 1;
            }
        }

        String peakMonthText = "Maior gasto em " + yearB + ": " + ptMonth(bestMonth);

        // saldo anual % (yearB vs yearA)
        BigDecimal incomeA = BigDecimal.ZERO, incomeB = BigDecimal.ZERO, expA = BigDecimal.ZERO, expB = BigDecimal.ZERO;
        for (var r : monthRows) {
            if (Objects.equals(r.getYear(), yearA)) {
                incomeA = incomeA.add(nvl(r.getIncome()));
                expA = expA.add(nvl(r.getExpense()));
            } else if (Objects.equals(r.getYear(), yearB)) {
                incomeB = incomeB.add(nvl(r.getIncome()));
                expB = expB.add(nvl(r.getExpense()));
            }
        }
        BigDecimal balA = incomeA.subtract(expA);
        BigDecimal balB = incomeB.subtract(expB);
        BigDecimal balPct = percent(balA, balB);
        String yearBalanceText = "Saldo anual: " + fmtPercent(balPct);

        return new ComparativeReportResponse.Insights(highlight, peakMonthText, yearBalanceText);
    }

    // ===================== Utils =====================

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal toBig(Number n) {
        if (n == null) return BigDecimal.ZERO;
        if (n instanceof BigDecimal bd) return bd;
        return new BigDecimal(n.toString());
    }

    private BigDecimal percent(BigDecimal a, BigDecimal b) {
        if (a == null || a.compareTo(BigDecimal.ZERO) == 0) {
            return (b == null || b.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return b.subtract(a)
                .multiply(BigDecimal.valueOf(100))
                .divide(a.abs(), 2, RoundingMode.HALF_UP);
    }

    private String fmtPercent(BigDecimal p) {
        // ex: +8,3% (você pode formatar melhor depois)
        return (p.signum() >= 0 ? "+" : "") + p + "%";
    }

    private String ptMonth(int month) {
        return switch (month) {
            case 1 -> "Janeiro";
            case 2 -> "Fevereiro";
            case 3 -> "Março";
            case 4 -> "Abril";
            case 5 -> "Maio";
            case 6 -> "Junho";
            case 7 -> "Julho";
            case 8 -> "Agosto";
            case 9 -> "Setembro";
            case 10 -> "Outubro";
            case 11 -> "Novembro";
            case 12 -> "Dezembro";
            default -> "Mês " + month;
        };
    }
}
