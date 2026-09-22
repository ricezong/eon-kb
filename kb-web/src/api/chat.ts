import { http, unwrap } from './client';
import type { ChatRequestPayload, ChatResponsePayload } from './types';

/** 同步聊天 */
export const chatApi = {
  /** POST /api/chat —— 一次性返回完整答案 */
  ask(payload: ChatRequestPayload): Promise<ChatResponsePayload> {
    return unwrap(http.post<ChatResponsePayload>('/chat', payload));
  }
};

/**
 * SSE 流式聊天客户端。
 *
 * <p>协议（见 ChatController#streamChat）：</p>
 * <ul>
 *   <li>{@code token} —— 增量文本片段</li>
 *   <li>{@code done}  —— 完整 ChatResponsePayload JSON</li>
 *   <li>{@code error} —— 错误信息文本</li>
 * </ul>
 *
 * <p>EventSource 只支持 GET，因此 kbIds 通过 query 传递。返回 close() 便于取消。</p>
 */
export interface StreamHandlers {
  onToken: (chunk: string) => void;
  onDone: (payload: ChatResponsePayload) => void;
  onError: (message: string) => void;
  /** 连接建立（可选） */
  onOpen?: () => void;
}

export function streamChat(
  question: string,
  knowledgeBaseIds: string[] | null | undefined,
  handlers: StreamHandlers
): () => void {
  const base =
    (import.meta.env.VITE_API_BASE as string | undefined) || '/api';
  const params = new URLSearchParams();
  params.set('question', question);
  (knowledgeBaseIds || []).forEach((id) =>
    params.append('knowledgeBaseIds', id)
  );
  const url = `${base}/chat/stream?${params.toString()}`;

  const es = new EventSource(url, { withCredentials: false });

  es.onopen = () => handlers.onOpen?.();

  es.addEventListener('token', (e: MessageEvent<string>) => {
    // SSE 会把纯文本 data 直接给到；后端每次 send 一个片段
    handlers.onToken(e.data ?? '');
  });

  es.addEventListener('done', (e: MessageEvent<string>) => {
    try {
      const payload = JSON.parse(e.data) as ChatResponsePayload;
      handlers.onDone(payload);
    } catch (err) {
      handlers.onError(`解析 done 事件失败: ${(err as Error).message}`);
    } finally {
      es.close();
    }
  });

  es.addEventListener('error', (e: MessageEvent<string> | Event) => {
    // 注意：EventSource 的 onerror 与 addEventListener('error') 都会触发；
    // 后端主动 send(name='error') 时是 MessageEvent，浏览器网络错误时是普通 Event。
    const msg =
      'data' in e && typeof (e as MessageEvent).data === 'string'
        ? ((e as MessageEvent).data as string)
        : 'SSE 连接异常';
    handlers.onError(msg);
    es.close();
  });

  return () => es.close();
}
