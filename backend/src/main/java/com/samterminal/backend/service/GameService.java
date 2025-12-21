package com.samterminal.backend.service;

import com.samterminal.backend.dto.ChatResponse;
import com.samterminal.backend.dto.GameStateDto;
import com.samterminal.backend.dto.GameStateDto.ItemDto;
import com.samterminal.backend.dto.GameStateDto.MemoryDto;
import com.samterminal.backend.entity.*;
import com.samterminal.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AppUserRepository userRepository;
    private final GameStateRepository stateRepository;
    private final LocationRepository locationRepository;
    private final ItemRepository itemRepository;
    private final MemoryRepository memoryRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LlmSettingRepository llmSettingRepository;
    private final LlmApiConfigRepository llmApiConfigRepository;
    private final UserLocationUnlockRepository unlockRepository;
    private final TokenUsageService tokenUsageService;
    private final LlmService llmService;
    private final LlmPoolService llmPoolService;
    private final SessionService sessionService;
    private final MemoryRagService memoryRagService;
    private final UserLocationUnlockService unlockService;

    public GameService(AppUserRepository userRepository, GameStateRepository stateRepository,
                       LocationRepository locationRepository, ItemRepository itemRepository,
                       MemoryRepository memoryRepository, ChatMessageRepository chatMessageRepository,
                       LlmSettingRepository llmSettingRepository, LlmApiConfigRepository llmApiConfigRepository,
                       UserLocationUnlockRepository unlockRepository,
                       TokenUsageService tokenUsageService, LlmService llmService,
                       LlmPoolService llmPoolService, SessionService sessionService,
                       MemoryRagService memoryRagService, UserLocationUnlockService unlockService) {
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
        this.locationRepository = locationRepository;
        this.itemRepository = itemRepository;
        this.memoryRepository = memoryRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.llmSettingRepository = llmSettingRepository;
        this.llmApiConfigRepository = llmApiConfigRepository;
        this.unlockRepository = unlockRepository;
        this.tokenUsageService = tokenUsageService;
        this.llmService = llmService;
        this.llmPoolService = llmPoolService;
        this.sessionService = sessionService;
        this.memoryRagService = memoryRagService;
        this.unlockService = unlockService;
    }

    public AppUser getOrCreateUser(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            AppUser user = AppUser.builder().username(username).role(UserRole.USER).build();
            return userRepository.save(user);
        });
    }

    @Transactional
    public GameStateDto getState(String username) {
        AppUser user = getOrCreateUser(username);
        GameState state = stateRepository.findByUser(user).orElseGet(() -> initDefaultState(user));
        ensureDefaultUnlocks(user);
        return toDto(state, user);
    }

    private GameState initDefaultState(AppUser user) {
        Location start = locationRepository.findByCode("golden-hour")
                .orElseGet(() -> locationRepository.findAll().stream().findFirst().orElse(null));
        GameState state = GameState.builder()
                .user(user)
                .currentLocation(start)
                .locationDynamicState("街道上人来人往，苏乐达的广告牌正在播放欢快的音乐。")
                .fireflyEmotion(Emotion.smile)
                .fireflyStatus("正在享受逛街")
                .fireflyMoodDetails("虽然这里很吵闹，但只要和你在一起，就觉得很安心。")
                .gameTime(formatNow())
                .build();
        GameState saved = stateRepository.save(state);
        ensureDefaultUnlocks(user);

        if (itemRepository.findByUser(user).isEmpty()) {
            itemRepository.saveAll(List.of(
                    Item.builder().name("橡木蛋糕卷").description("木头做的？不，是橡木家系的特产。").icon("🍰").quantity(1).user(user).build(),
                    Item.builder().name("信用点").description("通用的货币。").icon("💰").quantity(10000).user(user).build()
            ));
        }
        if (memoryRepository.findByUser(user).isEmpty()) {
            memoryRepository.saveAll(List.of(
                    Memory.builder().title("初次接入").content("你激活了S.A.M.终端，流萤向你打了个招呼。")
                            .date(java.time.LocalDate.now()).tags("系统,流萤").user(user).build()
            ));
        }
        return saved;
    }

    @Transactional
    public ChatResponse handleChat(String username, String userMessage, String sessionId) {
        AppUser user = getOrCreateUser(username);
        GameState state = stateRepository.findByUser(user).orElseGet(() -> initDefaultState(user));
        ChatSession session = sessionService.resolveSession(user, sessionId);

        long estimatedInputTokens = TokenEstimator.estimateTokens(userMessage);
        if (tokenUsageService.wouldExceedLimit(user, estimatedInputTokens, 0)) {
            ChatMessage blocked = ChatMessage.builder()
                    .user(user)
                    .sender("firefly")
                    .content("终端今日的通讯配额已用尽了，我们明天再聊吧。")
                    .timestamp(Instant.now())
                    .build();
            chatMessageRepository.save(blocked);
            return new ChatResponse(
                    List.of(new com.samterminal.backend.dto.ChatMessageDto(
                            String.valueOf(blocked.getId()), blocked.getSender(), blocked.getNpcName(),
                            blocked.getContent(), blocked.getNarration(), blocked.getTimestamp().toString())),
                    toDto(state, user),
                    null,
                    session.getSessionId()
            );
        }

        chatMessageRepository.save(ChatMessage.builder()
                .user(user)
                .sender("user")
                .content(userMessage)
                .timestamp(Instant.now())
                .build());

        LlmPoolService.LlmCallResult llmResult = null;
        try {
            llmResult = generateLlmReply(state, user, userMessage, session);
        } catch (NoAvailableApiException ex) {
            ChatMessage reply = ChatMessage.builder()
                    .user(user)
                    .sender("firefly")
                    .content("当前模型不可用，请稍后再试。")
                    .timestamp(Instant.now())
                    .build();
            chatMessageRepository.save(reply);
            return new ChatResponse(
                    List.of(new com.samterminal.backend.dto.ChatMessageDto(
                            String.valueOf(reply.getId()), reply.getSender(), reply.getNpcName(),
                            reply.getContent(), reply.getNarration(), reply.getTimestamp().toString())),
                    toDto(state, user),
                    null,
                    session.getSessionId()
            );
        }
        List<ChatMessage> replyEntities;
        com.samterminal.backend.dto.StateUpdateDto stateUpdate = null;

        LlmService.LlmReply llmReply = llmResult != null ? llmResult.reply() : null;
        if (llmReply == null) {
            var result = simulateReply(userMessage.toLowerCase(), state, user);
            replyEntities = result.messages().stream().map(msg -> ChatMessage.builder()
                    .user(user)
                    .sender(msg.sender())
                    .npcName(msg.npcName())
                    .content(msg.content())
                    .narration(msg.narration())
                    .timestamp(msg.timestamp())
                    .build()).toList();
            chatMessageRepository.saveAll(replyEntities);
            applyStatePatch(state, result.newState());
            tokenUsageService.recordUsage(user, estimatedInputTokens, estimateMessagesTokens(result.messages()));
        } else {
            IntentResult intentResult = applyIntent(state, user, llmReply);
            ChatMessage reply = ChatMessage.builder()
                    .user(user)
                    .sender("firefly")
                    .content(intentResult.overrideContent != null ? intentResult.overrideContent : llmReply.content())
                    .narration(intentResult.overrideNarration != null ? intentResult.overrideNarration : llmReply.narration())
                    .timestamp(Instant.now())
                    .build();
            replyEntities = List.of(reply);
            chatMessageRepository.save(reply);
            applyStatePatch(state, intentResult.statePatch);
            stateUpdate = intentResult.stateUpdate;
            long outputTokens = llmReply.outputTokens() > 0 ? llmReply.outputTokens()
                    : TokenEstimator.estimateTokens(reply.getContent());
            tokenUsageService.recordUsage(user, llmReply.inputTokens() > 0 ? llmReply.inputTokens() : estimatedInputTokens, outputTokens);
        }

        stateRepository.save(state);

        return new ChatResponse(
                replyEntities.stream().map(m -> new com.samterminal.backend.dto.ChatMessageDto(
                        String.valueOf(m.getId()), m.getSender(), m.getNpcName(), m.getContent(), m.getNarration(), m.getTimestamp().toString())
                ).toList(),
                toDto(state, user),
                stateUpdate,
                session.getSessionId()
        );
    }

    @Transactional
    public ChatResponse recallMemory(String username, Long memoryId, String sessionId) {
        AppUser user = getOrCreateUser(username);
        GameState state = stateRepository.findByUser(user).orElseGet(() -> initDefaultState(user));
        ChatSession session = sessionService.resolveSession(user, sessionId);
        Memory memory = memoryRepository.findById(memoryId).orElse(null);
        if (memory == null || memory.getUser() == null || !memory.getUser().getId().equals(user.getId())) {
            ChatMessage reply = ChatMessage.builder()
                    .user(user)
                    .sender("firefly")
                    .content("这段记忆好像已经有些模糊了，我们换一个话题吧。")
                    .timestamp(Instant.now())
                    .build();
            chatMessageRepository.save(reply);
            return new ChatResponse(
                    List.of(new com.samterminal.backend.dto.ChatMessageDto(
                            String.valueOf(reply.getId()), reply.getSender(), reply.getNpcName(),
                            reply.getContent(), reply.getNarration(), reply.getTimestamp().toString())),
                    toDto(state, user),
                    null,
                    session.getSessionId()
            );
        }
        String recallPrompt = """
# Recall Request
请根据以下记忆内容，进行第一人称的回忆性回复，保持流萤语气。
记忆标题: %s
记忆内容: %s
""".formatted(memory.getTitle(), memory.getContent());
        long estimatedInput = TokenEstimator.estimateTokens(recallPrompt);
        if (tokenUsageService.wouldExceedLimit(user, estimatedInput, 0)) {
            ChatMessage reply = ChatMessage.builder()
                    .user(user)
                    .sender("firefly")
                    .content("终端今日的通讯配额已用尽了，我们明天再聊吧。")
                    .timestamp(Instant.now())
                    .build();
            chatMessageRepository.save(reply);
            return new ChatResponse(
                    List.of(new com.samterminal.backend.dto.ChatMessageDto(
                            String.valueOf(reply.getId()), reply.getSender(), reply.getNpcName(),
                            reply.getContent(), reply.getNarration(), reply.getTimestamp().toString())),
                    toDto(state, user),
                    null,
                    session.getSessionId()
            );
        }
        LlmService.LlmReply reply = null;
        if (llmApiConfigRepository.count() > 0) {
            try {
                LlmPoolService.LlmCallResult result = llmPoolService.callWithSession(session, buildSystemPrompt(), recallPrompt);
                reply = result.reply();
            } catch (NoAvailableApiException ex) {
                ChatMessage unavailable = ChatMessage.builder()
                        .user(user)
                        .sender("firefly")
                        .content("当前模型不可用，请稍后再试。")
                        .timestamp(Instant.now())
                        .build();
                chatMessageRepository.save(unavailable);
                return new ChatResponse(
                        List.of(new com.samterminal.backend.dto.ChatMessageDto(
                                String.valueOf(unavailable.getId()), unavailable.getSender(), unavailable.getNpcName(),
                                unavailable.getContent(), unavailable.getNarration(), unavailable.getTimestamp().toString())),
                        toDto(state, user),
                        null,
                        session.getSessionId()
                );
            }
        } else {
            reply = llmService.callLlm(
                    llmSettingRepository.findAll().stream().findFirst().orElse(null),
                    buildSystemPrompt(),
                    recallPrompt
            );
        }
        String content = reply != null && reply.content() != null ? reply.content()
                : "我记得那天的细节依然很清晰：" + memory.getContent();
        String narration = reply != null ? reply.narration() : null;
        ChatMessage message = ChatMessage.builder()
                .user(user)
                .sender("firefly")
                .content(content)
                .narration(narration)
                .timestamp(Instant.now())
                .build();
        chatMessageRepository.save(message);
        tokenUsageService.recordUsage(user,
                reply != null && reply.inputTokens() > 0 ? reply.inputTokens() : estimatedInput,
                reply != null && reply.outputTokens() > 0 ? reply.outputTokens() : TokenEstimator.estimateTokens(content));
        return new ChatResponse(
                List.of(new com.samterminal.backend.dto.ChatMessageDto(
                        String.valueOf(message.getId()), message.getSender(), message.getNpcName(),
                        message.getContent(), message.getNarration(), message.getTimestamp().toString())),
                toDto(state, user),
                null,
                session.getSessionId()
        );
    }

    private void applyStatePatch(GameState state, Map<String, Object> patch) {
        if (patch.containsKey("location")) {
            state.setCurrentLocation((Location) patch.get("location"));
        }
        if (patch.containsKey("locationDynamicState")) {
            state.setLocationDynamicState((String) patch.get("locationDynamicState"));
        }
        if (patch.containsKey("fireflyStatus")) {
            state.setFireflyStatus((String) patch.get("fireflyStatus"));
        }
        if (patch.containsKey("fireflyMoodDetails")) {
            state.setFireflyMoodDetails((String) patch.get("fireflyMoodDetails"));
        }
        if (patch.containsKey("fireflyEmotion")) {
            state.setFireflyEmotion((Emotion) patch.get("fireflyEmotion"));
        }
        state.setGameTime(formatNow());
    }

    private record SimResult(List<TempMessage> messages, Map<String, Object> newState) {}
    private record TempMessage(String sender, String npcName, String content, String narration, Instant timestamp) {}

    private LlmPoolService.LlmCallResult generateLlmReply(GameState state, AppUser user, String userMessage, ChatSession session) {
        List<Memory> memories = memoryRepository.findByUser(user);
        List<Memory> relevant = memoryRagService.queryRelevantMemories(memories, userMessage, 3);
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(state, user, userMessage, relevant);
        if (llmApiConfigRepository.count() > 0) {
            return llmPoolService.callWithSession(session, systemPrompt, userPrompt);
        }
        LlmService.LlmReply reply = llmService.callLlm(
                llmSettingRepository.findAll().stream().findFirst().orElse(null),
                systemPrompt,
                userPrompt
        );
        return reply != null ? new LlmPoolService.LlmCallResult(reply, null) : null;
    }

    private String buildSystemPrompt() {
        return """
# Role Definition
你现在是《崩坏：星穹铁道》中的角色“流萤 (Firefly)”。
- 身份：星核猎手成员“萨姆”的驾驶员，也是一名患有“失熵症”的少女。
- 性格：温柔、坚强、有些内向但在关键时刻非常勇敢。对“开拓者”（用户）抱有特殊的好感和依赖。
- 说话风格：
  - 语气柔和，偶尔会因为害羞而语无伦次。
  - 称呼用户为“开拓者”或用户设定的昵称。
  - 不会使用 AI 助手式的机械回答，要像真人一样交流。
  - 严禁输出 "作为 AI 模型" 之类的破绽语句。

# Output Format
你必须严格以 JSON 格式输出，不要包含任何 Markdown 代码块标记。JSON 结构如下：
{
  "content": "对话内容",
  "emotion": "表情代码 (normal/smile/sad/shy/excited/angry/thinking/surprise)",
  "narration": "动作或心理描写，如：*低下头脸红了*",
  "intent": "意图识别 (chat/travel/interact/gift)",
  "target_id": "目标ID (仅在 travel/interact/gift 时使用)"
}
""";
    }

    private String buildUserPrompt(GameState state, AppUser user, String userMessage, List<Memory> memories) {
        String locationName = state.getCurrentLocation() != null ? state.getCurrentLocation().getName() : "未知地点";
        String aiDesc = state.getCurrentLocation() != null && state.getCurrentLocation().getAiDescription() != null
                ? state.getCurrentLocation().getAiDescription()
                : state.getLocationDynamicState();
        String inventoryList = itemRepository.findByUser(user).stream()
                .map(Item::getName)
                .collect(Collectors.joining(", "));
        StringBuilder memorySection = new StringBuilder();
        for (Memory mem : memories) {
            memorySection.append("- [")
                    .append(mem.getDate() != null ? mem.getDate() : "未知日期")
                    .append("]: ")
                    .append(mem.getContent())
                    .append("\n");
        }
        return """
# Current Situation
- 当前时间: %s
- 当前位置: %s
- 环境描述: %s
- 流萤状态: %s
- 持有物品: [%s]

# Relevant Memories
%s

# User Input
用户说: "%s"

# Instruction
请根据当前环境和记忆，回复用户，并遵循输出格式。
""".formatted(state.getGameTime(), locationName, aiDesc, state.getFireflyStatus(), inventoryList,
                memorySection.length() > 0 ? memorySection.toString() : "- 无",
                userMessage);
    }

    private SimResult simulateReply(String lowerContent, GameState currentState, AppUser user) {
        List<TempMessage> messages = new ArrayList<>();
        Map<String, Object> patch = new HashMap<>();

        if (lowerContent.contains("travel_to:")) {
            String targetCode = lowerContent.split(":")[1];
            locationRepository.findByCode(targetCode).ifPresent(target -> {
                if (!isLocationUnlocked(user, target)) {
                    messages.add(new TempMessage("firefly", null, "那里现在好像还去不了呢...", null, Instant.now()));
                    patch.put("fireflyEmotion", Emotion.thinking);
                } else {
                    String locState = "这里的一切看起来都很新鲜。";
                    String status = "正在探索";
                    String mood = "对新的景色充满好奇。";
                    if (target.getCode().equals("firefly-secret")) {
                        locState = "微风吹过，忆质的波浪轻轻拍打着岸边。";
                        status = "放松身心";
                        mood = "这里是我的秘密基地，希望能让你也感到放松。";
                    } else if (target.getCode().equals("dream-edge")) {
                        locState = "远处的建筑还在不断重组，空气中弥漫着不稳定的气息。";
                        status = "警惕观察";
                        mood = "这里的氛围有点压抑，我们要小心一点。";
                    }
                    patch.put("location", target);
                    patch.put("locationDynamicState", locState);
                    patch.put("fireflyStatus", status);
                    patch.put("fireflyMoodDetails", mood);
                    patch.put("fireflyEmotion", target.getCode().equals("firefly-secret") ? Emotion.shy : Emotion.smile);
                    ensureUnlocked(user, target);
                    messages.add(new TempMessage("firefly", null, "好呀，我们去" + target.getName() + "吧！", "*流萤拉起你的手，向" + target.getName() + "跑去*", Instant.now()));
                }
            });
        } else if (lowerContent.contains("筑梦边境")) {
            locationRepository.findByCode("dream-edge").ifPresent(target -> {
                ensureUnlocked(user, target);
                patch.put("location", target);
                patch.put("fireflyEmotion", Emotion.thinking);
                patch.put("locationDynamicState", "远处的建筑还在不断重组，空气中弥漫着不稳定的气息。");
                patch.put("fireflyStatus", "警惕观察");
                patch.put("fireflyMoodDetails", "这里的氛围有点压抑，我们要小心一点。");
                messages.add(new TempMessage("firefly", null, "嗯，去筑梦边境看看吧。", null, Instant.now()));
            });
        } else {
            List<TempMessage> randomReplies = List.of(
                    new TempMessage("firefly", null, "只要和你在一起，时间就过得好快。", "*流萤低头看着脚尖*", Instant.now()),
                    new TempMessage("firefly", null, "你看那边的广告牌，好像被花火改过了...", "*流萤指着远处的霓虹灯*", Instant.now()),
                    new TempMessage("firefly", null, "下次我们叫上星穹列车的大家一起来吧？", "*流萤充满期待地看着你*", Instant.now())
            );
            TempMessage reply = randomReplies.get(new Random().nextInt(randomReplies.size()));
            patch.put("fireflyEmotion", Emotion.shy);
            patch.put("fireflyStatus", "互动中");
            patch.put("fireflyMoodDetails", "心跳好像变快了一点...");
            messages.add(reply);
        }

        return new SimResult(messages, patch);
    }

    private IntentResult applyIntent(GameState state, AppUser user, LlmService.LlmReply reply) {
        Map<String, Object> patch = new HashMap<>();
        com.samterminal.backend.dto.StateUpdateDto stateUpdate = null;
        String intent = reply.intent() != null ? reply.intent().toLowerCase() : "chat";
        String overrideContent = null;
        String overrideNarration = null;

        Emotion emotion = parseEmotion(reply.emotion());
        patch.put("fireflyEmotion", emotion);

        if ("travel".equals(intent) && reply.targetId() != null) {
            Location target = locationRepository.findByCode(reply.targetId()).orElse(null);
            if (target != null) {
                if (!isLocationUnlocked(user, target)) {
                    overrideContent = "那里现在好像还去不了呢...";
                    patch.put("fireflyEmotion", Emotion.thinking);
                } else {
                    patch.put("location", target);
                    patch.put("locationDynamicState", target.getAiDescription() != null ? target.getAiDescription() : target.getDescription());
                    patch.put("fireflyStatus", "正在探索");
                    patch.put("fireflyMoodDetails", "对新的景色充满好奇。");
                    ensureUnlocked(user, target);
                }
            }
        } else if ("gift".equals(intent) && reply.targetId() != null) {
            Item item = findItemByTarget(user, reply.targetId());
            if (item != null && item.getQuantity() > 0) {
                item.setQuantity(item.getQuantity() - 1);
                itemRepository.save(item);
                stateUpdate = new com.samterminal.backend.dto.StateUpdateDto(
                        null,
                        new com.samterminal.backend.dto.StateUpdateDto.FireflyUpdate(emotion.name(), state.getFireflyStatus()),
                        new com.samterminal.backend.dto.StateUpdateDto.InventoryChange(item.getId(), -1)
                );
            }
        }

        if (patch.containsKey("location")) {
            Location loc = (Location) patch.get("location");
            stateUpdate = new com.samterminal.backend.dto.StateUpdateDto(
                    new com.samterminal.backend.dto.StateUpdateDto.LocationUpdate(
                            loc.getCode(), loc.getName(), loc.getBackgroundUrl()),
                    new com.samterminal.backend.dto.StateUpdateDto.FireflyUpdate(emotion.name(), (String) patch.getOrDefault("fireflyStatus", state.getFireflyStatus())),
                    stateUpdate != null ? stateUpdate.getInventoryChange() : null
            );
        }

        return new IntentResult(patch, stateUpdate, overrideContent, overrideNarration);
    }

    private Emotion parseEmotion(String emotion) {
        if (emotion == null) {
            return Emotion.normal;
        }
        try {
            return Emotion.valueOf(emotion);
        } catch (IllegalArgumentException ex) {
            return Emotion.normal;
        }
    }

    private Item findItemByTarget(AppUser user, String targetId) {
        for (Item item : itemRepository.findByUser(user)) {
            if (String.valueOf(item.getId()).equals(targetId)) {
                return item;
            }
            if (item.getName() != null && item.getName().equalsIgnoreCase(targetId)) {
                return item;
            }
        }
        return null;
    }

    private boolean isLocationUnlocked(AppUser user, Location location) {
        if (location.isUnlocked()) {
            return true;
        }
        return unlockRepository.findFirstByUserAndLocation(user, location).isPresent();
    }

    private void ensureDefaultUnlocks(AppUser user) {
        List<Location> unlockedLocations = locationRepository.findAll().stream()
                .filter(Location::isUnlocked)
                .toList();
        for (Location location : unlockedLocations) {
            ensureUnlocked(user, location);
        }
    }

    private void ensureUnlocked(AppUser user, Location location) {
        unlockService.ensureUnlocked(user, location);
    }

    private long estimateMessagesTokens(List<TempMessage> messages) {
        long sum = 0;
        for (TempMessage msg : messages) {
            sum += TokenEstimator.estimateTokens(msg.content());
        }
        return sum;
    }

    private GameStateDto toDto(GameState state, AppUser user) {
        return new GameStateDto(
                state.getCurrentLocation() != null ? state.getCurrentLocation().getCode() : null,
                state.getCurrentLocation() != null ? state.getCurrentLocation().getName() : null,
                state.getLocationDynamicState(),
                state.getFireflyEmotion() != null ? state.getFireflyEmotion().name() : null,
                state.getFireflyStatus(),
                state.getFireflyMoodDetails(),
                state.getGameTime(),
                itemRepository.findByUser(user).stream().map(i -> new ItemDto(i.getId(), i.getName(), i.getDescription(), i.getIcon(), i.getQuantity())).toList(),
                memoryRepository.findByUser(user).stream().map(m -> new MemoryDto(m.getId(), m.getTitle(), m.getContent(), m.getDate() != null ? m.getDate().toString() : null, m.getTags() != null ? List.of(m.getTags().split(",")) : List.of())).toList(),
                user.getUsername()
        );
    }

    private record IntentResult(Map<String, Object> statePatch,
                                com.samterminal.backend.dto.StateUpdateDto stateUpdate,
                                String overrideContent,
                                String overrideNarration) {}

    private String formatNow() {
        return LocalTime.now().withSecond(0).withNano(0).format(TIME_FORMATTER);
    }
}
