package com.cp.friend.service;

import com.cp.friend.model.Message;
import com.cp.friend.service.strategy.MessageContentStrategy;
import com.cp.friend.service.strategy.MessageContentStrategyResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class MessageContentStrategyResolverTest {
    private MessageContentStrategy strategy(Message.MessageType type, String result) {
        return new MessageContentStrategy() {
            public Message.MessageType supportedType() { return type; }
            public boolean supports(Message.MessageType candidate) { return candidate == type; }
            public String process(String raw) { return result; }
        };
    }

    @Test void registersImplementationWithoutRequiringAbstractSuperclass() {
        var text = strategy(Message.MessageType.TEXT, "processed");
        var resolver = new MessageContentStrategyResolver(List.of(text));
        assertSame(text, resolver.resolve(Message.MessageType.TEXT));
        assertEquals("processed", resolver.resolve(Message.MessageType.TEXT).process("raw"));
    }

    @Test void selectsEachRegisteredStrategyByItsDeclaredType() {
        var text = strategy(Message.MessageType.TEXT, "text");
        var image = strategy(Message.MessageType.IMAGE, "image-url");
        var resolver = new MessageContentStrategyResolver(List.of(image, text));
        assertSame(text, resolver.resolve(Message.MessageType.TEXT));
        assertSame(image, resolver.resolve(Message.MessageType.IMAGE));
    }

    @Test void rejectsUnregisteredTypeWithExistingBadRequestContract() {
        var resolver = new MessageContentStrategyResolver(List.of());
        var error = assertThrows(ResponseStatusException.class,
                () -> resolver.resolve(Message.MessageType.TEXT));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Unsupported messageType: TEXT", error.getReason());
    }
}
