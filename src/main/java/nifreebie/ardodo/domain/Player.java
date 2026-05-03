package nifreebie.ardodo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "players")
@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Player {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; 

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    private List<GameSession> sessions;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}