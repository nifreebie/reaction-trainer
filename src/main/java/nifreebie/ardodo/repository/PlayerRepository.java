package nifreebie.ardodo.repository;

import nifreebie.ardodo.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    Optional<Player> findByName(String name);
    boolean existsByName(String name);

}



