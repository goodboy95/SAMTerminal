package com.samterminal.backend.config;

import com.samterminal.backend.entity.*;
import com.samterminal.backend.repository.*;
import com.samterminal.backend.service.AdminAccountService;
import com.samterminal.backend.service.LlmSettingMigrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(StarDomainRepository domainRepo, LocationRepository locationRepo,
                           AppUserRepository userRepo,
                           GameStateRepository stateRepo, ItemRepository itemRepo,
                           MemoryRepository memoryRepo, NpcCharacterRepository characterRepository,
                           AdminAccountService adminAccountService,
                           LlmSettingMigrationService migrationService) {
        return args -> {
            if (domainRepo.count() == 0) {
                StarDomain penacony = domainRepo.save(StarDomain.builder().code("penacony").name("匹诺康尼").description("盛会之星，美梦的国度。").coordX(70).coordY(50).color("text-purple-400").build());
                StarDomain jarilo = domainRepo.save(StarDomain.builder().code("jarilo").name("雅利洛-VI").description("冰雪覆盖的星球，存护的领地。").coordX(30).coordY(30).color("text-blue-400").build());
                domainRepo.saveAll(List.of(
                        StarDomain.builder().code("herta").name("黑塔空间站").description("天才俱乐部黑塔女士的私人财产。").coordX(20).coordY(70).color("text-indigo-400").build(),
                        StarDomain.builder().code("luofu").name("仙舟「罗浮」").description("巡猎的巨舰，云骑军的驻地。").coordX(80).coordY(20).color("text-teal-400").build()
                ));

                locationRepo.saveAll(List.of(
                        Location.builder().code("golden-hour").name("黄金的时刻").description("永远停留在午夜之前的繁华都市，霓虹灯闪烁，是匹诺康尼最热闹的梦境区域。").backgroundStyle("bg-gradient-to-br from-yellow-600 via-orange-500 to-red-500").coordX(50).coordY(50).unlocked(true).domain(penacony).build(),
                        Location.builder().code("dream-edge").name("筑梦边境").description("梦境与现实交汇的边缘，可以看到巨大的都市倒影，正在建设中的梦境荒野。").backgroundStyle("bg-gradient-to-b from-indigo-900 to-purple-800").coordX(80).coordY(30).unlocked(true).domain(penacony).build(),
                        Location.builder().code("firefly-secret").name("流梦礁·秘密基地").description("只有流萤知道的安静角落，可以看到蓝色的忆质海洋，远离了喧嚣。").backgroundStyle("bg-gradient-to-t from-blue-900 to-slate-800").coordX(20).coordY(70).unlocked(true).domain(penacony).build(),
                        Location.builder().code("hotel-lobby").name("白日梦酒店").description("现实中的酒店大堂，金碧辉煌，是入梦前的必经之地。").backgroundStyle("bg-gradient-to-r from-slate-900 to-slate-700").coordX(30).coordY(20).unlocked(false).domain(penacony).build(),
                        Location.builder().code("admin-district").name("行政区").description("贝洛伯格的上层区，永冬之城的中心，巨大的齿轮雕塑矗立在广场中央。").backgroundStyle("bg-gradient-to-b from-slate-200 to-slate-400").coordX(50).coordY(50).unlocked(true).domain(jarilo).build()
                ));
            }

            adminAccountService.syncAdmins();
            migrationService.migrateIfNeeded();

            if (characterRepository.count() == 0) {
                characterRepository.save(NpcCharacter.builder().name("花火").prompt("淘气的愚者，喜欢恶作剧。")
                        .role("trickster").build());
            }

            // create default user state if exists
            userRepo.findByUsername("trailblazer").ifPresent(user -> {
                if (stateRepo.findByUser(user).isEmpty()) {
                    Location start = locationRepo.findByCode("golden-hour").orElse(null);
                    GameState st = GameState.builder().user(user).currentLocation(start)
                            .locationDynamicState("街道上人来人往，苏乐达的广告牌正在播放欢快的音乐。")
                            .fireflyEmotion(Emotion.smile)
                            .fireflyStatus("正在享受逛街")
                            .fireflyMoodDetails("虽然这里很吵闹，但只要和你在一起，就觉得很安心。")
                            .gameTime("21:45")
                            .build();
                    stateRepo.save(st);
                    itemRepo.saveAll(List.of(
                            Item.builder().name("橡木蛋糕卷").description("木头做的？不，是橡木家系的特产。").icon("🍰").quantity(2).user(user).build(),
                            Item.builder().name("信用点").description("通用的货币。").icon("💰").quantity(20000).user(user).build()
                    ));
                    memoryRepo.saveAll(List.of(
                            Memory.builder().title("天台的约定").content("在黄金的时刻边缘，流萤向你展示了她的秘密基地，并约定下次再见。")
                                    .date(java.time.LocalDate.now().minusDays(1)).tags("重要,流萤").user(user).build(),
                            Memory.builder().title("花火的恶作剧").content("那个戴面具的愚者似乎对你们很有兴趣...")
                                    .date(java.time.LocalDate.now()).tags("NPC,花火").user(user).build()
                    ));
                }
            });
        };
    }
}
