package com.aidb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "ai.api.key=test-key",
    "ai.provider=anthropic"
})
class AiDatabaseAssistantApplicationTests {
    @Test
    void contextLoads() {}
}
