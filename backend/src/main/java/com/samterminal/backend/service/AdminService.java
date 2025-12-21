package com.samterminal.backend.service;

import com.samterminal.backend.dto.CharacterAdminRequest;
import com.samterminal.backend.dto.FireflyAssetRequest;
import com.samterminal.backend.dto.LocationAdminRequest;
import com.samterminal.backend.dto.StarDomainAdminRequest;
import com.samterminal.backend.dto.UserUsageDto;
import com.samterminal.backend.dto.UserUsageResponse;
import com.samterminal.backend.entity.*;
import com.samterminal.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminService {
    private final FireflyAssetRepository assetRepository;
    private final NpcCharacterRepository characterRepository;
    private final LocationRepository locationRepository;
    private final StarDomainRepository domainRepository;
    private final LlmSettingRepository llmSettingRepository;
    private final LlmApiConfigRepository llmApiConfigRepository;
    private final AppUserRepository userRepository;
    private final TokenUsageService tokenUsageService;
    private final UserTokenUsageRepository usageRepository;
    private final UserTokenLimitRepository limitRepository;

    public AdminService(FireflyAssetRepository assetRepository, NpcCharacterRepository characterRepository,
                        LocationRepository locationRepository, StarDomainRepository domainRepository,
                        LlmSettingRepository llmSettingRepository, LlmApiConfigRepository llmApiConfigRepository,
                        AppUserRepository userRepository,
                        TokenUsageService tokenUsageService, UserTokenUsageRepository usageRepository,
                        UserTokenLimitRepository limitRepository) {
        this.assetRepository = assetRepository;
        this.characterRepository = characterRepository;
        this.locationRepository = locationRepository;
        this.domainRepository = domainRepository;
        this.llmSettingRepository = llmSettingRepository;
        this.llmApiConfigRepository = llmApiConfigRepository;
        this.userRepository = userRepository;
        this.tokenUsageService = tokenUsageService;
        this.usageRepository = usageRepository;
        this.limitRepository = limitRepository;
    }

    @Transactional
    public void saveAssets(FireflyAssetRequest request) {
        for (Map.Entry<String, String> entry : request.getAssets().entrySet()) {
            Emotion emotion = Emotion.valueOf(entry.getKey());
            assetRepository.findByEmotion(emotion)
                    .map(asset -> { asset.setUrl(entry.getValue()); return asset; })
                    .orElseGet(() -> assetRepository.save(FireflyAsset.builder().emotion(emotion).url(entry.getValue()).build()));
        }
    }

    @Transactional(readOnly = true)
    public List<FireflyAsset> listAssets() {
        return assetRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CharacterAdminRequest> listCharacters() {
        return characterRepository.findAll().stream()
                .map(c -> new CharacterAdminRequest(
                        c.getId(),
                        c.getName(),
                        c.getRole(),
                        c.getPrompt(),
                        c.getDescription(),
                        c.getAvatarUrl()))
                .toList();
    }

    @Transactional
    public NpcCharacter upsertCharacter(CharacterAdminRequest request) {
        NpcCharacter character = request.getId() != null
                ? characterRepository.findById(request.getId()).orElseGet(NpcCharacter::new)
                : new NpcCharacter();
        character.setName(request.getName());
        character.setRole(request.getRole());
        character.setPrompt(request.getPrompt());
        character.setDescription(request.getDescription());
        character.setAvatarUrl(request.getAvatarUrl());
        return characterRepository.save(character);
    }

    @Transactional(readOnly = true)
    public List<LocationAdminRequest> listLocations() {
        return locationRepository.findAll().stream()
                .map(l -> new LocationAdminRequest(
                        l.getId(),
                        l.getCode(),
                        l.getName(),
                        l.getDescription(),
                        l.getAiDescription(),
                        l.getBackgroundStyle(),
                        l.getBackgroundUrl(),
                        l.getCoordX(),
                        l.getCoordY(),
                        l.isUnlocked(),
                        l.getDomain() != null ? l.getDomain().getCode() : null))
                .toList();
    }

    @Transactional
    public Location upsertLocation(LocationAdminRequest request) {
        Location location = request.getId() != null
                ? locationRepository.findById(request.getId()).orElseGet(Location::new)
                : new Location();
        location.setCode(request.getCode());
        location.setName(request.getName());
        location.setDescription(request.getDescription());
        location.setAiDescription(request.getAiDescription());
        location.setBackgroundStyle(request.getBackgroundStyle());
        location.setBackgroundUrl(request.getBackgroundUrl());
        location.setCoordX(request.getCoordX());
        location.setCoordY(request.getCoordY());
        location.setUnlocked(request.isUnlocked());
        if (request.getDomainCode() != null) {
            domainRepository.findByCode(request.getDomainCode()).ifPresent(location::setDomain);
        }
        return locationRepository.save(location);
    }

    @Transactional(readOnly = true)
    public List<StarDomainAdminRequest> listDomains() {
        return domainRepository.findAll().stream()
                .map(d -> new StarDomainAdminRequest(
                        d.getId(),
                        d.getCode(),
                        d.getName(),
                        d.getDescription(),
                        d.getAiDescription(),
                        d.getCoordX(),
                        d.getCoordY(),
                        d.getColor()))
                .toList();
    }

    @Transactional
    public StarDomain upsertDomain(StarDomainAdminRequest request) {
        StarDomain domain = request.getId() != null
                ? domainRepository.findById(request.getId()).orElseGet(StarDomain::new)
                : new StarDomain();
        domain.setCode(request.getCode());
        domain.setName(request.getName());
        domain.setDescription(request.getDescription());
        domain.setAiDescription(request.getAiDescription());
        domain.setCoordX(request.getCoordX());
        domain.setCoordY(request.getCoordY());
        domain.setColor(request.getColor());
        return domainRepository.save(domain);
    }

    @Transactional
    public LlmSetting saveLlm(LlmSetting setting) {
        if (setting.getId() == null && llmSettingRepository.count() > 0) {
            setting.setId(llmSettingRepository.findAll().get(0).getId());
        }
        LlmSetting saved = llmSettingRepository.save(setting);
        syncToApiConfig(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public LlmSetting getLlm() {
        return llmSettingRepository.findAll().stream().findFirst().orElse(null);
    }

    private void syncToApiConfig(LlmSetting setting) {
        if (setting == null || setting.getBaseUrl() == null || setting.getModelName() == null) {
            return;
        }
        LlmApiConfig config = llmApiConfigRepository.findAll().stream()
                .findFirst()
                .orElseGet(LlmApiConfig::new);
        config.setName(config.getName() != null ? config.getName() : "Migrated LLM");
        config.setBaseUrl(setting.getBaseUrl());
        if (setting.getApiKey() != null) {
            config.setApiKey(setting.getApiKey());
        }
        config.setModelName(setting.getModelName());
        config.setTemperature(setting.getTemperature());
        if (config.getRole() == null) {
            config.setRole(LlmApiRole.PRIMARY);
        }
        if (config.getStatus() == null) {
            config.setStatus(LlmApiStatus.ACTIVE);
        }
        if (config.getTokenUsed() == null) {
            config.setTokenUsed(0L);
        }
        if (config.getFailureCount() == null) {
            config.setFailureCount(0);
        }
        if (config.getMaxLoad() == null) {
            config.setMaxLoad(30);
        }
        llmApiConfigRepository.save(config);
    }

    @Transactional
    public void deleteDomain(Long id) {
        domainRepository.deleteById(id);
    }

    @Transactional
    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

    @Transactional
    public void deleteCharacter(Long id) {
        characterRepository.deleteById(id);
    }

    @Transactional
    public void batchSaveDomains(List<StarDomainAdminRequest> domains) {
        for (StarDomainAdminRequest request : domains) {
            upsertDomain(request);
        }
    }

    @Transactional
    public void batchSaveLocations(List<LocationAdminRequest> locations) {
        for (LocationAdminRequest request : locations) {
            upsertLocation(request);
        }
    }

    @Transactional
    public void batchSaveCharacters(List<CharacterAdminRequest> characters) {
        for (CharacterAdminRequest request : characters) {
            upsertCharacter(request);
        }
    }

    @Transactional(readOnly = true)
    public UserUsageResponse listUserUsage() {
        List<UserUsageDto> users = userRepository.findAll().stream()
                .map(user -> {
                    var usage = usageRepository.findByUser(user);
                    long input = usage.map(UserTokenUsage::getInputTokens).orElse(0L);
                    long output = usage.map(UserTokenUsage::getOutputTokens).orElse(0L);
                    Long custom = limitRepository.findByUser(user).map(UserTokenLimit::getCustomLimit).orElse(null);
                    return new UserUsageDto(user.getId(), user.getUsername(), input, output, custom);
                })
                .toList();
        return new UserUsageResponse(tokenUsageService.getGlobalLimit(), users);
    }

    @Transactional
    public void updateUserLimit(Long userId, Long limit) {
        Optional<AppUser> user = userRepository.findById(userId);
        user.ifPresent(u -> tokenUsageService.setUserLimit(u, limit));
    }

    @Transactional
    public void updateGlobalLimit(Long limit) {
        tokenUsageService.setGlobalLimit(limit);
    }
}
