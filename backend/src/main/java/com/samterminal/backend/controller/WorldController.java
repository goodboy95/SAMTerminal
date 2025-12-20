package com.samterminal.backend.controller;

import com.samterminal.backend.dto.LocationDto;
import com.samterminal.backend.dto.MapResponse;
import com.samterminal.backend.dto.StarDomainDto;
import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.Location;
import com.samterminal.backend.repository.AppUserRepository;
import com.samterminal.backend.repository.FireflyAssetRepository;
import com.samterminal.backend.repository.LocationRepository;
import com.samterminal.backend.repository.StarDomainRepository;
import com.samterminal.backend.repository.UserLocationUnlockRepository;
import com.samterminal.backend.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/world")
public class WorldController {

    private final StarDomainRepository domainRepository;
    private final LocationRepository locationRepository;
    private final JwtService jwtService;
    private final UserLocationUnlockRepository unlockRepository;
    private final AppUserRepository userRepository;
    private final FireflyAssetRepository assetRepository;

    public WorldController(StarDomainRepository domainRepository, LocationRepository locationRepository,
                           JwtService jwtService, UserLocationUnlockRepository unlockRepository,
                           AppUserRepository userRepository, FireflyAssetRepository assetRepository) {
        this.domainRepository = domainRepository;
        this.locationRepository = locationRepository;
        this.jwtService = jwtService;
        this.unlockRepository = unlockRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
    }

    @GetMapping("/map")
    public ResponseEntity<MapResponse> map(HttpServletRequest request) {
        Set<String> unlockedCodes = resolveUser(request)
                .map(user -> {
                    ensureDefaultUnlocks(user);
                    return unlockRepository.findByUser(user).stream()
                            .map(u -> u.getLocation().getCode())
                            .collect(Collectors.toSet());
                })
                .orElse(Set.of());
        List<StarDomainDto> domains = domainRepository.findAll().stream()
                .map(d -> new StarDomainDto(d.getCode(), d.getName(), d.getDescription(), d.getCoordX(), d.getCoordY(), d.getColor()))
                .toList();
        List<LocationDto> locations = locationRepository.findAll().stream()
                .map(l -> new LocationDto(
                        l.getCode(),
                        l.getName(),
                        l.getDescription(),
                        l.getBackgroundStyle(),
                        l.getBackgroundUrl(),
                        l.getCoordX(),
                        l.getCoordY(),
                        l.isUnlocked() || unlockedCodes.contains(l.getCode()),
                        l.getDomain() != null ? l.getDomain().getCode() : null))
                .toList();
        return ResponseEntity.ok(new MapResponse(domains, locations));
    }

    @GetMapping("/assets/firefly")
    public ResponseEntity<?> fireflyAssets() {
        return ResponseEntity.ok(assetRepository.findAll());
    }

    private java.util.Optional<AppUser> resolveUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String username = jwtService.extractUsername(header.substring(7));
                return userRepository.findByUsername(username);
            } catch (Exception ignored) {
            }
        }
        return java.util.Optional.empty();
    }

    private void ensureDefaultUnlocks(AppUser user) {
        List<Location> defaults = locationRepository.findAll().stream()
                .filter(Location::isUnlocked)
                .toList();
        for (Location location : defaults) {
            unlockRepository.findFirstByUserAndLocation(user, location)
                    .orElseGet(() -> unlockRepository.save(com.samterminal.backend.entity.UserLocationUnlock.builder()
                            .user(user)
                            .location(location)
                            .unlockedAt(java.time.Instant.now())
                            .build()));
        }
    }
}
