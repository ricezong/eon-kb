package cn.kong.kb.api;

import cn.kong.kb.api.dto.ChatRequest;
import cn.kong.kb.api.dto.ChatResponse;
import cn.kong.kb.chat.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 向知识库提问（同步，一次性返回完整答案）。
     */
    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "question must not be blank"));
        }
        List<UUID> kbIds = parseKnowledgeBaseIds(request.knowledgeBaseIds());
        ChatResponse response = chatService.chat(request.question(), kbIds);
        return ResponseEntity.ok(response);
    }

    /**
     * 提问并通过 Server-Sent Events 流式返回答案。
     *
     * <p>事件协议：</p>
     * <ul>
     *   <li>{@code token} — 增量的答案文本片段</li>
     *   <li>{@code done}  — 最终载荷，包含完整答案 + 引用</li>
     *   <li>{@code error} — 错误信息</li>
     * </ul>
     *
     * <p>浏览器使用示例：</p>
     * <pre>{@code
     * const es = new EventSource("/api/chat/stream?question=" + encodeURIComponent(q));
     * es.addEventListener("token", e => appendChunk(e.data));
     * es.addEventListener("done",  e => { const resp = JSON.parse(e.data); renderCitations(resp.citations); es.close(); });
     * es.addEventListener("error", e => handleError(e.data));
     * }</pre>
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam String question,
                                 @RequestParam(required = false) List<UUID> knowledgeBaseIds) {
        if (question == null || question.isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("question must not be blank"));
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(ignored);
            }
            return emitter;
        }
        return chatService.streamChat(question, knowledgeBaseIds);
    }

    /**
     * 解析字符串形式的知识库 ID 列表为 UUID 列表。
     */
    private List<UUID> parseKnowledgeBaseIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(UUID::fromString).toList();
    }
}
