package com.trazza.trazzawebservice.matchmaking.infrastructure.persistence.jpa.repositories;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByMatchProposalIdOrderBySentAt(Long matchProposalId);

    /** Deletes the chat of a proposal (the trip ended). Returns how many messages were removed. */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from Message m where m.matchProposalId = :matchProposalId")
    int deleteByProposal(@Param("matchProposalId") Long matchProposalId);
}
