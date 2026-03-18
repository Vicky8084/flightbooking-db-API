package db_api.db_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_segments")
@Data
public class FlightSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_flight_id", nullable = false)
    private Flight parentFlight; // The connecting flight

    @ManyToOne
    @JoinColumn(name = "segment_flight_id", nullable = false)
    private Flight segmentFlight; // Individual flight in connection

    private Integer sequenceOrder; // 1,2,3 for multiple segments

    private Integer layoverDuration; // in minutes (null for last segment)

    // Getters and Setters
}
