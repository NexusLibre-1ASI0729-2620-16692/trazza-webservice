package com.trazza.trazzawebservice.shared.infrastructure.transactions;

import com.trazza.trazzawebservice.shared.domain.model.Result;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Commands that change several aggregates commit only when they succeed: a failure marks the current
 * transaction as rollback-only (needed with row locks such as {@code SELECT ... FOR UPDATE}).
 */
public final class Transactions {

    private Transactions() {
    }

    public static <T> Result<T> rollbackOnFailure(Result<T> result) {
        if (result.isFailure() && TransactionSynchronizationManager.isActualTransactionActive())
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }
}
