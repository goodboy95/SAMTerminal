package com.samterminal.backend.controller;

import com.samterminal.backend.dto.*;
import com.samterminal.backend.entity.*;
import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.UserRole;
import com.samterminal.backend.repository.AppUserRepository;
import com.samterminal.backend.service.AdminService;
import com.samterminal.backend.service.AuthService;
import com.samterminal.backend.service.LlmApiConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final AppUserRepository userRepository;
    private final LlmApiConfigService llmApiConfigService;

    public AdminController(AdminService adminService, AuthService authService, AppUserRepository userRepository,
                           LlmApiConfigService llmApiConfigService) {
        this.adminService = adminService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.llmApiConfigService = llmApiConfigService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        String token = authService.login(request);
        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        }
        return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername(), "role", user.getRole().name()));
    }

    @GetMapping("/assets/firefly")
    public ResponseEntity<?> getAssets() {
        return ResponseEntity.ok(adminService.listAssets());
    }

    @PostMapping("/assets/firefly")
    public ResponseEntity<?> saveAssets(@RequestBody FireflyAssetRequest request) {
        adminService.saveAssets(request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/world/locations")
    public ResponseEntity<?> locations() {
        return ResponseEntity.ok(adminService.listLocations());
    }

    @PostMapping("/world/locations")
    public ResponseEntity<?> upsertLocation(@RequestBody LocationAdminRequest location) {
        return ResponseEntity.ok(adminService.upsertLocation(location));
    }

    @GetMapping("/world/domains")
    public ResponseEntity<?> domains() {
        return ResponseEntity.ok(adminService.listDomains());
    }

    @PostMapping("/world/domains")
    public ResponseEntity<?> upsertDomain(@RequestBody StarDomainAdminRequest domain) {
        return ResponseEntity.ok(adminService.upsertDomain(domain));
    }

    @GetMapping("/world/characters")
    public ResponseEntity<?> characters() {
        return ResponseEntity.ok(adminService.listCharacters());
    }

    @PostMapping("/world/characters")
    public ResponseEntity<?> upsertCharacter(@RequestBody CharacterAdminRequest character) {
        return ResponseEntity.ok(adminService.upsertCharacter(character));
    }

    @PostMapping("/world/domains/batch")
    public ResponseEntity<?> batchDomains(@RequestBody List<StarDomainAdminRequest> domains) {
        adminService.batchSaveDomains(domains);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/world/locations/batch")
    public ResponseEntity<?> batchLocations(@RequestBody List<LocationAdminRequest> locations) {
        adminService.batchSaveLocations(locations);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/world/characters/batch")
    public ResponseEntity<?> batchCharacters(@RequestBody List<CharacterAdminRequest> characters) {
        adminService.batchSaveCharacters(characters);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/world/domains/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable Long id) {
        adminService.deleteDomain(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/world/locations/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
        adminService.deleteLocation(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/world/characters/{id}")
    public ResponseEntity<?> deleteCharacter(@PathVariable Long id) {
        adminService.deleteCharacter(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/system/llm")
    public ResponseEntity<?> getLlm() {
        LlmSetting s = adminService.getLlm();
        if (s == null) {
            return ResponseEntity.ok(new LlmSettingResponse(null, null, null));
        }
        return ResponseEntity.ok(new LlmSettingResponse(s.getBaseUrl(), s.getModelName(), s.getTemperature()));
    }

    @PostMapping("/system/llm")
    public ResponseEntity<?> saveLlm(@RequestBody LlmSetting setting) {
        adminService.saveLlm(setting);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/system/llm/test")
    public ResponseEntity<?> testLlm() {
        return ResponseEntity.ok(Map.of("status", "connected"));
    }

    @GetMapping("/system/llm-apis")
    public ResponseEntity<?> listLlmApis() {
        return ResponseEntity.ok(llmApiConfigService.listConfigs());
    }

    @PostMapping("/system/llm-apis")
    public ResponseEntity<?> createLlmApi(@RequestBody LlmApiConfigRequest request) {
        return ResponseEntity.ok(llmApiConfigService.createConfig(request));
    }

    @PutMapping("/system/llm-apis/{id}")
    public ResponseEntity<?> updateLlmApi(@PathVariable Long id, @RequestBody LlmApiConfigRequest request) {
        return ResponseEntity.ok(llmApiConfigService.updateConfig(id, request));
    }

    @DeleteMapping("/system/llm-apis/{id}")
    public ResponseEntity<?> deleteLlmApi(@PathVariable Long id) {
        llmApiConfigService.deleteConfig(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/system/llm-apis/{id}/reset-tokens")
    public ResponseEntity<?> resetTokens(@PathVariable Long id) {
        return ResponseEntity.ok(llmApiConfigService.resetTokens(id));
    }

    @PostMapping("/system/llm-apis/{id}/test")
    public ResponseEntity<?> testLlmApi(@PathVariable Long id) {
        boolean ok = llmApiConfigService.testConfig(id);
        return ResponseEntity.ok(Map.of("status", ok ? "connected" : "failed"));
    }

    @GetMapping("/users/usage")
    public ResponseEntity<?> usage() {
        return ResponseEntity.ok(adminService.listUserUsage());
    }

    @PostMapping("/settings/global-limit")
    public ResponseEntity<?> updateGlobalLimit(@RequestBody TokenLimitRequest request) {
        adminService.updateGlobalLimit(request.getLimit());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/users/{id}/limit")
    public ResponseEntity<?> updateUserLimit(@PathVariable Long id, @RequestBody TokenLimitRequest request) {
        adminService.updateUserLimit(id, request.getLimit());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
