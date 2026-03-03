import { useState, useRef, useEffect } from 'react';
import { api } from '../api/client';
import { CitationCard } from './CitationCard';
import { StreamingMessage } from './StreamingMessage';
import type { ChatMessage, Citation } from '../types';

interface Props {
  workspaceId: string;
  sessionId: string | null;
  onSessionCreated: (sessionId: string) => void;
}

export function ChatView({ workspaceId, sessionId, onSessionCreated }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [streamContent, setStreamContent] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [useRag, setUseRag] = useState(false);
  const [showDebug, setShowDebug] = useState(false);
  const [citations, setCitations] = useState<Citation[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (sessionId) {
      api.getMessages(sessionId).then(setMessages).catch(() => {});
    } else {
      setMessages([]);
    }
  }, [sessionId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamContent]);

  const sendMessage = async () => {
    if (!input.trim() || loading) return;
    const msg = input.trim();
    setInput('');

    const userMsg: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'USER',
      content: msg,
      confidence: null,
      citations: [],
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);
    setStreamContent('');
    setIsStreaming(true);

    api.chatStream(workspaceId, msg, sessionId || undefined, useRag, (event) => {
      if (event.type === 'error') {
        setIsStreaming(false);
        setLoading(false);
        return;
      }
      setStreamContent((prev) => prev + event.content);
    });

    // Fallback: also fire sync request for persistence + citations
    try {
      const res = await api.chat(workspaceId, msg, sessionId || undefined, useRag);
      if (!sessionId) onSessionCreated(res.sessionId);
      setCitations(res.citations || []);

      const assistantMsg: ChatMessage = {
        id: res.messageId,
        role: 'ASSISTANT',
        content: res.content,
        confidence: res.confidence,
        citations: res.citations || [],
        createdAt: res.timestamp,
      };
      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err) {
      const errorMsg: ChatMessage = {
        id: crypto.randomUUID(),
        role: 'ASSISTANT',
        content: `Error: ${err instanceof Error ? err.message : 'Unknown error'}`,
        confidence: null,
        citations: [],
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setLoading(false);
      setIsStreaming(false);
      setStreamContent('');
    }
  };

  return (
    <div className="chat-view">
      <div className="chat-toolbar">
        <label className="toggle">
          <input type="checkbox" checked={useRag} onChange={(e) => setUseRag(e.target.checked)} />
          RAG Mode
        </label>
        <label className="toggle">
          <input type="checkbox" checked={showDebug} onChange={(e) => setShowDebug(e.target.checked)} />
          Debug
        </label>
      </div>

      <div className="messages">
        {messages.map((msg) => (
          <div key={msg.id} className={`message ${msg.role.toLowerCase()}`}>
            <div className="message-role">{msg.role === 'USER' ? 'You' : 'Assistant'}</div>
            <div className="message-content">{msg.content}</div>
            {msg.confidence != null && (
              <div className="message-confidence">
                Confidence: {(msg.confidence * 100).toFixed(0)}%
              </div>
            )}
          </div>
        ))}
        {isStreaming && <StreamingMessage content={streamContent} isStreaming />}
        <div ref={messagesEndRef} />
      </div>

      {showDebug && citations.length > 0 && (
        <div className="debug-panel">
          <h4>Retrieved Chunks</h4>
          {citations.map((c, i) => (
            <CitationCard key={i} citation={c} />
          ))}
        </div>
      )}

      <div className="chat-input">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              sendMessage();
            }
          }}
          placeholder="Type a message..."
          rows={2}
          disabled={loading}
        />
        <button onClick={sendMessage} disabled={loading || !input.trim()} className="btn-primary">
          {loading ? 'Sending...' : 'Send'}
        </button>
      </div>
    </div>
  );
}
