package nifreebie.ardodo.mapper;

import nifreebie.ardodo.domain.Player;
import nifreebie.ardodo.dto.PlayerDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlayerMapper {
    PlayerDTO toPlayerDTO(Player player);
    Player toPlayer(PlayerDTO playerDTO);
}
