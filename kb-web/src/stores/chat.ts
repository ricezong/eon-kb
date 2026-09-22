import { defineStore } from 'pinia';
import { ref } from 'vue';
import { streamChat, chatApi } from '@/api';
import type { Citation } from '@/api/types';

export type MessageRole = 'user' | 'assistant' | 'system';
export type MessageStatus = 'pending' | 'streaming' | 'done' | 'error';

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  status: MessageStatus;
  citations?: Citation[];
  createdAt: number;
  errorMessage?: string;
}

/**
 * 会话状态：多会话（左侧列表）+ 每会话独立消息流。
 * 首版聚焦单会话 + 历史持久化到 localStorage，方便刷新后不丢消息。
 */
export interface ChatSession {
  id: string;
  title: string;
  createdAt: number;
  updatedAt: number;
  messages: ChatMessage[];
}

const STORAGE_KEY = 'eon-kb:chat-sessions';
const MAX_SESSIONS = 30;

function loadSessions(): ChatSession[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as ChatSession[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function persist(sessions: ChatSession[]) {
  try {
    // 只持久化最近 30 个会话
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions.slice(0, MAX_SESSIONS)));
  } catch { /* quota */ }
}

function newId(): string {
  return (crypto as any).randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>(loadSessions());
  const activeSessionId = ref<string | null>(sessions.value[0]?.id ?? null);
  const sending = ref(false);
  const activeKbIds = ref<string[]>([]); // 当前对话生效的知识库过滤

  // 关闭当前流的取消函数
  let cancelStream: (() => void) | null = null;

  function getActive(): ChatSession | null {
    return sessions.value.find((s) => s.id === activeSessionId.value) ?? null;
  }

  function createSession(firstQuestion?: string): ChatSession {
    const now = Date.now();
    const s: ChatSession = {
      id: newId(),
      title: firstQuestion?.slice(0, 24) || '新对话',
      createdAt: now,
      updatedAt: now,
      messages: []
    };
    sessions.value.unshift(s);
    activeSessionId.value = s.id;
    persist(sessions.value);
    return s;
  }

  function selectSession(id: string) {
    activeSessionId.value = id;
  }

  function removeSession(id: string) {
    const idx = sessions.value.findIndex((s) => s.id === id);
    if (idx < 0) return;
    sessions.value.splice(idx, 1);
    if (activeSessionId.value === id) {
      activeSessionId.value = sessions.value[0]?.id ?? null;
    }
    persist(sessions.value);
  }

  function clearAll() {
    sessions.value = [];
    activeSessionId.value = null;
    persist(sessions.value);
  }

  function renameSession(id: string, title: string) {
    const s = sessions.value.find((x) => x.id === id);
    if (s) { s.title = title; persist(sessions.value); }
  }

  /**
   * 发送问题：优先走 SSE 流式；若 EventSource 不可用或出错则回退同步接口。
   */
  async function send(question: string, opts: { stream?: boolean } = {}) {
    const q = question.trim();
    if (!q || sending.value) return;

    let session = getActive();
    if (!session) session = createSession(q);
    if (!session.messages.length && session.title === '新对话') {
      session.title = q.slice(0, 24);
    }

    const userMsg: ChatMessage = {
      id: newId(), role: 'user', content: q, status: 'done', createdAt: Date.now()
    };
    const botMsg: ChatMessage = {
      id: newId(), role: 'assistant', content: '', status: 'streaming', createdAt: Date.now()
    };
    session.messages.push(userMsg, botMsg);
    session.updatedAt = Date.now();
    persist(sessions.value);

    sending.value = true;
    const kbIds = activeKbIds.value.length ? [...activeKbIds.value] : null;

    const useStream = opts.stream !== false && typeof EventSource !== 'undefined';

    if (useStream) {
      await new Promise<void>((resolve) => {
        cancelStream = streamChat(q, kbIds, {
          onToken: (t) => {
            botMsg.content += t;
          },
          onDone: (payload) => {
            // done 事件里带完整答案与引用；以 payload.answer 为准（防止 token 拼接误差）
            if (payload.answer) botMsg.content = payload.answer;
            botMsg.citations = payload.citations || [];
            botMsg.status = 'done';
            session!.updatedAt = Date.now();
            persist(sessions.value);
            sending.value = false;
            cancelStream = null;
            resolve();
          },
          onError: (msg) => {
            botMsg.status = 'error';
            botMsg.errorMessage = msg;
            // 尝试回退到同步接口
            fallback();
            resolve();
          }
        });
      });
    } else {
      await fallback();
    }

    async function fallback() {
      try {
        botMsg.status = 'streaming';
        const resp = await chatApi.ask({ question: q, knowledgeBaseIds: kbIds });
        botMsg.content = resp.answer;
        botMsg.citations = resp.citations;
        botMsg.status = 'done';
      } catch (e) {
        botMsg.status = 'error';
        botMsg.errorMessage = (e as Error).message;
      } finally {
        sending.value = false;
        cancelStream = null;
        session!.updatedAt = Date.now();
        persist(sessions.value);
      }
    }
  }

  function stop() {
    if (cancelStream) {
      cancelStream();
      cancelStream = null;
    }
    const s = getActive();
    if (s) {
      const last = s.messages[s.messages.length - 1];
      if (last && last.status === 'streaming') last.status = 'done';
    }
    sending.value = false;
  }

  function clearActiveMessages() {
    const s = getActive();
    if (s) { s.messages = []; persist(sessions.value); }
  }

  return {
    sessions, activeSessionId, sending, activeKbIds,
    getActive, createSession, selectSession, removeSession, clearAll, renameSession,
    send, stop, clearActiveMessages
  };
});
