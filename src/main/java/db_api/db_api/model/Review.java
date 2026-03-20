package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import db_api.db_api.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    private Integer rating; // 1-5

    @Column(length = 1000)
    private String comment;

    private String title;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.PENDING; // PENDING, APPROVED, REJECTED

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
}