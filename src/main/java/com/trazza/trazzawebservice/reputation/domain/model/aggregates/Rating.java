package com.trazza.trazzawebservice.reputation.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "ratings")
@Getter
public class Rating {

    public static final int MAX_COMMENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private Long raterUserId;

    @Column(nullable = false)
    private Long ratedUserId;

    @Column(nullable = false)
    private int score;

    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    @Column(nullable = false)
    private Instant createdAt;

    protected Rating() {
    }

    public Rating(Long tripId, Long raterUserId, Long ratedUserId, int score, String comment, Instant now) {
        this.tripId = tripId;
        this.raterUserId = raterUserId;
        this.ratedUserId = ratedUserId;
        this.score = score;
        this.comment = comment == null || comment.isBlank() ? null : comment.trim();
        this.createdAt = now;
    }
}