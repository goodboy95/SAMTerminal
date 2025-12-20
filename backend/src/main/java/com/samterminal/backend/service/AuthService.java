package com.samterminal.backend.service;

import com.samterminal.backend.dto.AuthRequest;
import com.samterminal.backend.entity.*;
import com.samterminal.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AppUserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final GameStateRepository gameStateRepository;
    private final LocationRepository locationRepository;
    private final ItemRepository itemRepository;
    private final MemoryRepository memoryRepository;

    public AuthService(AppUserRepository userRepository, PasswordEncoder encoder, JwtService jwtService,
                       GameStateRepository gameStateRepository, LocationRepository locationRepository,
                       ItemRepository itemRepository, MemoryRepository memoryRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.gameStateRepository = gameStateRepository;
        this.locationRepository = locationRepository;
        this.itemRepository = itemRepository;
        this.memoryRepository = memoryRepository;
    }

    public String login(AuthRequest request) {
        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getPassword() != null && !encoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return jwtService.generateToken(user.getUsername(), Map.of("role", user.getRole().name()));
    }

    @Transactional
    public String register(AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("User already exists");
        }
        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();
        userRepository.save(user);

        Location start = locationRepository.findByCode("golden-hour")
                .orElseGet(() -> locationRepository.findAll().stream().findFirst().orElse(null));

        GameState state = GameState.builder()
                .user(user)
                .currentLocation(start)
                .locationDynamicState("街道上人来人往，苏乐达的广告牌正在播放欢快的音乐。")
                .fireflyEmotion(Emotion.smile)
                .fireflyStatus("正在享受逛街")
                .fireflyMoodDetails("虽然这里很吵闹，但只要和你在一起，就觉得很安心。")
                .gameTime(LocalTime.now().withSecond(0).withNano(0).format(TIME_FORMATTER))
                .build();
        gameStateRepository.save(state);

        itemRepository.saveAll(List.of(
                Item.builder().name("橡木蛋糕卷").description("木头做的？不，是橡木家系的特产。").icon("🍰").quantity(2).user(user).build(),
                Item.builder().name("信用点").description("通用的货币。").icon("💰").quantity(20000).user(user).build()
        ));

        memoryRepository.saveAll(List.of(
                Memory.builder().title("天台的约定").content("在黄金的时刻边缘，流萤向你展示了她的秘密基地，并约定下次再见。")
                        .date(LocalDate.now().minusDays(1)).tags("重要,流萤").user(user).build(),
                Memory.builder().title("花火的恶作剧").content("那个戴面具的愚者似乎对你们很有兴趣...")
                        .date(LocalDate.now()).tags("NPC,花火").user(user).build()
        ));

        return jwtService.generateToken(user.getUsername(), Map.of("role", user.getRole().name()));
    }
}
