package com.aiwechat.aichat.service;

import com.aiwechat.aichat.model.entity.ChatRecord;
import com.aiwechat.aichat.repository.ChatRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRecordService {

    private final ChatRecordRepository chatRecordRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @CacheEvict(value = "chatHistory", key = "#userId")
    public void saveChatRecord(String userId, String sessionId, String question, String answer, List<String> sources) {
        LocalDateTime now = LocalDateTime.now();

        ChatRecord userRecord = new ChatRecord();
        userRecord.setUserId(userId);
        userRecord.setSessionId(sessionId);
        userRecord.setQuestion(question);
        userRecord.setAnswer(null);
        userRecord.setRole("user");
        userRecord.setSources(sourcesToJson(sources));
        userRecord.setCreatedAt(now);
        chatRecordRepository.insert(userRecord);

        ChatRecord assistantRecord = new ChatRecord();
        assistantRecord.setUserId(userId);
        assistantRecord.setSessionId(sessionId);
        assistantRecord.setQuestion(null);
        assistantRecord.setAnswer(answer);
        assistantRecord.setRole("assistant");
        assistantRecord.setSources(sourcesToJson(sources));
        assistantRecord.setCreatedAt(now.plusSeconds(1));
        chatRecordRepository.insert(assistantRecord);
    }

    private String sourcesToJson(List<String> sources) {
        if (sources == null || sources.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return null;
        }
    }
}
