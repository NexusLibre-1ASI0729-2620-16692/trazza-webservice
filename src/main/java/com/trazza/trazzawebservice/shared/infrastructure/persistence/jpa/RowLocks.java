package com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Locks a row until the current transaction ends ({@code SELECT ... FOR UPDATE}) and reloads its state, so two
 * requests cannot take the last free slot of a route at the same time.
 */
@Component
public class RowLocks {

    @PersistenceContext
    private EntityManager entityManager;

    public <T> T lock(Class<T> type, Object id) {
        var entity = entityManager.find(type, id);
        if (entity == null) return null;
        entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return entity;
    }
}
