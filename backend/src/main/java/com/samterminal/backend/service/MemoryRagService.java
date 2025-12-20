package com.samterminal.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samterminal.backend.entity.Memory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class MemoryRagService {
    private static final String COLLECTION_NAME = "sam_memories";
    private static final int EMBEDDING_DIM = 64;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String chromaUrl;

    private String collectionId;

    public MemoryRagService(ObjectMapper objectMapper,
                            @Value("${chroma.url:http://localhost:8000}") String chromaUrl) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.chromaUrl = chromaUrl;
    }

    public List<Memory> queryRelevantMemories(List<Memory> memories, String query, int limit) {
        if (memories == null || memories.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            ensureCollection();
            upsertMemories(memories);
            return queryChroma(memories, query, limit);
        } catch (Exception ex) {
            // fallback to simple matching
            return fallbackMatch(memories, query, limit);
        }
    }

    private void ensureCollection() throws Exception {
        if (collectionId != null) {
            return;
        }
        String listUrl = chromaUrl + "/api/v1/collections";
        ResponseEntity<String> listRes = restTemplate.getForEntity(listUrl, String.class);
        List<?> collections = objectMapper.readValue(listRes.getBody(), List.class);
        for (Object item : collections) {
            Map<?, ?> map = (Map<?, ?>) item;
            if (COLLECTION_NAME.equals(map.get("name"))) {
                collectionId = String.valueOf(map.get("id"));
                return;
            }
        }
        Map<String, Object> payload = Map.of("name", COLLECTION_NAME);
        ResponseEntity<Map> createRes = restTemplate.postForEntity(listUrl, payload, Map.class);
        collectionId = String.valueOf(createRes.getBody().get("id"));
    }

    private void upsertMemories(List<Memory> memories) throws Exception {
        List<String> ids = new ArrayList<>();
        List<List<Double>> embeddings = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();

        for (Memory memory : memories) {
            ids.add("mem-" + memory.getId());
            embeddings.add(embeddingFor(memory.getContent()));
            documents.add(memory.getContent());
            Map<String, Object> meta = new HashMap<>();
            meta.put("title", memory.getTitle());
            meta.put("date", memory.getDate() != null ? memory.getDate().toString() : null);
            metadatas.add(meta);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("ids", ids);
        payload.put("embeddings", embeddings);
        payload.put("documents", documents);
        payload.put("metadatas", metadatas);

        String upsertUrl = chromaUrl + "/api/v1/collections/" + collectionId + "/upsert";
        restTemplate.postForEntity(upsertUrl, payload, String.class);
    }

    private List<Memory> queryChroma(List<Memory> memories, String query, int limit) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query_embeddings", List.of(embeddingFor(query)));
        payload.put("n_results", Math.max(1, limit));

        String queryUrl = chromaUrl + "/api/v1/collections/" + collectionId + "/query";
        ResponseEntity<String> resp = restTemplate.postForEntity(queryUrl, payload, String.class);
        Map<?, ?> data = objectMapper.readValue(resp.getBody(), Map.class);
        List<List<String>> ids = (List<List<String>>) data.get("ids");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<String> idSet = new HashSet<>(ids.get(0));
        List<Memory> result = new ArrayList<>();
        for (Memory memory : memories) {
            if (idSet.contains("mem-" + memory.getId())) {
                result.add(memory);
            }
        }
        return result;
    }

    private List<Memory> fallbackMatch(List<Memory> memories, String query, int limit) {
        String lower = query.toLowerCase();
        return memories.stream()
                .sorted(Comparator.comparingInt(m -> -score(m, lower)))
                .filter(m -> score(m, lower) > 0)
                .limit(limit)
                .toList();
    }

    private int score(Memory memory, String lower) {
        int score = 0;
        if (memory.getTitle() != null && memory.getTitle().toLowerCase().contains(lower)) {
            score += 2;
        }
        if (memory.getContent() != null && memory.getContent().toLowerCase().contains(lower)) {
            score += 3;
        }
        return score;
    }

    private List<Double> embeddingFor(String text) {
        double[] vec = new double[EMBEDDING_DIM];
        if (text != null) {
            String[] tokens = text.toLowerCase().split("\\s+");
            for (String token : tokens) {
                int idx = Math.abs(token.hashCode()) % EMBEDDING_DIM;
                vec[idx] += 1.0;
            }
        }
        double norm = 0.0;
        for (double v : vec) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        List<Double> result = new ArrayList<>(EMBEDDING_DIM);
        for (double v : vec) {
            result.add(norm == 0.0 ? 0.0 : v / norm);
        }
        return result;
    }
}
