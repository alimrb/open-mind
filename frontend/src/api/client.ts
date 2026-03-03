import type {
  Workspace,
  Session,
  ChatMessage,
  ChatResponse,
  HealthResponse,
  KnowledgeUploadResponse,
} from '../types';

const API_BASE = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(error.message || 'Request failed');
  }
  return res.json();
}

export const api = {
  // Health
  health: () => request<HealthResponse>('/health'),

  // Workspaces
  listWorkspaces: () => request<Workspace[]>('/workspaces'),
  createWorkspace: (name: string, description?: string) =>
    request<Workspace>('/workspaces', {
      method: 'POST',
      body: JSON.stringify({ name, description }),
    }),
  deleteWorkspace: (id: string) =>
    fetch(`${API_BASE}/workspaces/${id}`, { method: 'DELETE' }),

  // Sessions
  listSessions: (workspaceId: string) =>
    request<Session[]>(`/workspaces/${workspaceId}/sessions`),
  getMessages: (sessionId: string) =>
    request<ChatMessage[]>(`/sessions/${sessionId}/messages`),

  // Chat
  chat: (workspaceId: string, message: string, sessionId?: string, useRag = false) =>
    request<ChatResponse>('/chat', {
      method: 'POST',
      body: JSON.stringify({ workspaceId, sessionId, message, useRag }),
    }),

  chatStream: (
    workspaceId: string,
    message: string,
    sessionId?: string,
    useRag = false,
    onEvent?: (event: { type: string; content: string }) => void,
  ): EventSource => {
    // SSE via POST requires fetch + ReadableStream
    const controller = new AbortController();
    fetch(`${API_BASE}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ workspaceId, sessionId, message, useRag }),
      signal: controller.signal,
    }).then(async (res) => {
      const reader = res.body?.getReader();
      const decoder = new TextDecoder();
      if (!reader) return;

      let buffer = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (data) {
              try {
                const parsed = JSON.parse(data);
                onEvent?.(parsed);
              } catch {
                onEvent?.({ type: 'text', content: data });
              }
            }
          }
        }
      }
    });

    return { close: () => controller.abort() } as unknown as EventSource;
  },

  // Knowledge
  uploadKnowledge: (workspaceId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return fetch(`${API_BASE}/workspaces/${workspaceId}/knowledge`, {
      method: 'POST',
      body: formData,
    }).then((res) => res.json() as Promise<KnowledgeUploadResponse>);
  },
};
