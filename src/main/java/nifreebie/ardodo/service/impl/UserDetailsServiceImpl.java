package nifreebie.ardodo.service.impl;

import lombok.RequiredArgsConstructor;
import nifreebie.ardodo.config.PlayerDetails;
import nifreebie.ardodo.domain.Player;
import nifreebie.ardodo.repository.PlayerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final PlayerRepository playerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Player player = playerRepository.findByName(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return PlayerDetails.builder()
                .username(player.getName())
                .password(player.getPasswordHash())
                .id(player.getId())
                .authorities(List.of())
                .build();
    }
}
