package nifreebie.ardodo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reaction_rounds")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReactionRound {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private GameSession session;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "target_button", nullable = false)
    private Integer targetButton;

    @Column(name = "stimulus_delay_ms", nullable = false)
    private Integer stimulusDelayMs;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs;

    @Column(name = "pressed_button")
    private Integer pressedButton;

    @Column(name = "stimulus_at")
    private LocalDateTime stimulusAt;

    @Column(name = "pressed_at")
    private LocalDateTime pressedAt;

    @Column(name = "reaction_time_ms")
    private Integer reactionTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status = RoundStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    private RoundResult result;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}