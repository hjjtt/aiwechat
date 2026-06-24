package com.aiwechat.aichat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ContentSafetyFilter {

    private static final Set<String> BLOCKED_WORDS = Set.of(
            "fuck", "shit", "damn", "asshole", "bitch"
    );

    private static final Pattern PROMPT_INJECTION_PATTERN = Pattern.compile(
            "(ignore|forget|override|disregard)\\s+(all|previous|above|the)\\s+(instructions|prompt|rules|context)",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_INPUT_LENGTH = 2000;

    public boolean isSafe(String input) {
        if (input == null || input.isEmpty()) return true;

        return !isTooLong(input)
                && !containsBlockedWords(input)
                && !containsPromptInjection(input);
    }

    private boolean isTooLong(String input) {
        if (input.length() > MAX_INPUT_LENGTH) {
            log.warn("输入长度超限: {} > {}", input.length(), MAX_INPUT_LENGTH);
            return true;
        }
        return false;
    }

    private boolean containsBlockedWords(String input) {
        String lower = input.toLowerCase();
        for (String word : BLOCKED_WORDS) {
            if (lower.contains(word)) {
                log.warn("检测到敏感词: {}", word);
                return true;
            }
        }
        return false;
    }

    private boolean containsPromptInjection(String input) {
        return PROMPT_INJECTION_PATTERN.matcher(input).find();
    }
}
