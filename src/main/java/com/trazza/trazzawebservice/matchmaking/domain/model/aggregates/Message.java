package com.trazza.trazzawebservice.matchmaking.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/** Chat message between the carrier and the merchant of a proposal. Messages are deleted when the trip ends. */
@Entity
@Table(name = "messages")
@Getter
public class Message {

    public static final int MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long matchProposalId;

    @Column(nullable = false)
    private Long senderUserId;

    @Column(nullable = false, length = MAX_LENGTH)
    private String content;

    @Column(nullable = false)
    private Instant sentAt;

    private Instant readAt;

    protected Message() {
    }

    public Message(Long matchProposalId, Long senderUserId, String content, Instant sentAt) {
        this.matchProposalId = matchProposalId;
        this.senderUserId = senderUserId;
        this.content = content.trim();
        this.sentAt = sentAt;
    }

    public void markRead(Instant now) {
        if (readAt == null) readAt = now;
    }
}
