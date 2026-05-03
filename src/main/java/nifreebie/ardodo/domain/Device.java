package nifreebie.ardodo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "device_token", nullable = false)
    private String deviceToken;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "is_online")
    private Boolean isOnline;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    public void prePersist() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
        if (isOnline == null) {
            isOnline = false;
        }
    }
}
