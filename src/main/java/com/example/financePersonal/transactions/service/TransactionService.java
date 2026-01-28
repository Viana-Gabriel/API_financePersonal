package com.example.financePersonal.transactions.service;

import com.example.financePersonal.categories.model.Category;
import com.example.financePersonal.categories.repository.CategoryRepository;
import com.example.financePersonal.common.exception.ApiException;
import com.example.financePersonal.security.CurrentUser;
import com.example.financePersonal.transactions.dto.*;
import com.example.financePersonal.transactions.model.Transaction;
import com.example.financePersonal.transactions.model.TransactionType;
import com.example.financePersonal.transactions.repository.TransactionRepository;
import com.example.financePersonal.users.model.User;
import com.example.financePersonal.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public TransactionResponse create(TransactionCreateRequest req) {
        UUID userId = CurrentUser.principal().userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findByIdAndUser_Id(req.categoryId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        }

        String desc = req.description().trim();
        String notes = (req.notes() == null || req.notes().isBlank()) ? null : req.notes().trim();

        Transaction tx = Transaction.builder()
                .user(user)
                .category(category)
                .type(req.type())
                .amount(req.amount())
                .date(req.date())
                .description(desc)
                .notes(notes)
                .deletedAt(null)
                .build();

        Transaction saved = transactionRepository.save(tx);

        // Reusar query de detalhes pra devolver tudo padronizado (com category fields)
        return transactionRepository.findDetails(userId, saved.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load created transaction"));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(
            Integer year,
            UUID categoryId,
            TransactionType type,
            String q,
            int page,
            int size
    ) {
        UUID userId = CurrentUser.principal().userId();

        int currentYear = java.time.Year.now().getValue();
        if (year < 2000 || year > currentYear + 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "year is invalid");
        }

        String normalizedQ = (q == null) ? null : q.trim();
        if (normalizedQ != null && normalizedQ.isBlank()) normalizedQ = null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "date")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return transactionRepository
                .searchPage(userId, year, type, categoryId, normalizedQ, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id) {
        UUID userId = CurrentUser.principal().userId();

        return transactionRepository.findDetails(userId, id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @Transactional
    public TransactionResponse update(UUID id, TransactionUpdateRequest req) {
        UUID userId = CurrentUser.principal().userId();

        Transaction tx = transactionRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (tx.getDeletedAt() != null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Transaction not found");
        }

        if (req.type() != null) tx.setType(req.type());
        if (req.amount() != null) tx.setAmount(req.amount());
        if (req.date() != null) tx.setDate(req.date());

        if (req.description() != null) {
            String d = req.description().trim();
            if (d.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Description cannot be blank");
            tx.setDescription(d);
        }

        if (req.notes() != null) {
            tx.setNotes(req.notes().isBlank() ? null : req.notes().trim());
        }

        if (req.categoryId() != null) {
            // se quiser permitir "remover categoria", mande categoryId = null (mas record não distingue bem)
            // aqui: categoryId != null => setar categoria
            Category cat = categoryRepository.findByIdAndUser_Id(req.categoryId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
            tx.setCategory(cat);
        }

        transactionRepository.save(tx);

        return transactionRepository.findDetails(userId, id)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load updated transaction"));
    }

    @Transactional
    public void delete(UUID id) {
        UUID userId = CurrentUser.principal().userId();

        Transaction tx = transactionRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (tx.getDeletedAt() == null) {
            tx.setDeletedAt(OffsetDateTime.now());
            transactionRepository.save(tx);
        }
    }

    private TransactionResponse toResponse(Transaction t) {
        var c = t.getCategory();

        return new TransactionResponse(
                t.getId(),
                t.getDate(),
                t.getDescription(),
                t.getType(),
                t.getAmount(),
                c != null ? c.getId() : null,
                c != null ? c.getName() : null,
                c != null ? c.getColorHex() : null,
                c != null ? c.getIcon() : null,
                t.getCreatedAt()
        );
    }
}
