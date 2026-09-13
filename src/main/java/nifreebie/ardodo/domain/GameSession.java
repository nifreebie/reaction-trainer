package nifreebie.ardodo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "game_sessions")
@Getter
@Setter
public class GameSession {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false)
    @JoinColumn(name = "device_id")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameMode mode;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "rounds_count")
    private Integer roundsCount;

    @Column(name = "avg_reaction_ms")
    private Integer avgAnswerTimeMs;

    @Column(name = "best_reaction_ms")
    private Integer bestAnswerTimeMs;

    @Column(name = "false_starts_count")
    private Integer unusedLegacyCount;

    @Column(name = "current_round", nullable = false)
    private Integer currentRound = 1;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs;

    @Column(name = "game_seed")
    private Long gameSeed;

    @Column(name = "hits_count")
    private Integer correctAnswersCount;

    @Column(name = "misses_count")
    private Integer missedAnswersCount;

    @Column(name = "wrong_buttons_count")
    private Integer incorrectAnswersCount;

    @Column(name = "total_reaction_ms")
    private Integer totalAnswerTimeMs;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<ArithmeticRound> rounds;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (startedAt == null) startedAt = LocalDateTime.now();
    }
}
