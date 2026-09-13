package nifreebie.ardodo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "arithmetic_rounds")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArithmeticRound {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private GameSession session;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "first_number", nullable = false)
    private Integer firstNumber;

    @Column(name = "second_number", nullable = false)
    private Integer secondNumber;

    @Column(name = "correct_answer", nullable = false)
    private Integer correctAnswer;

    @Column(name = "entered_answer")
    private Integer enteredAnswer;

    @Column(name = "shown_at", nullable = false)
    private LocalDateTime shownAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "answer_time_ms")
    private Integer answerTimeMs;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status = RoundStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    private RoundResult result;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
