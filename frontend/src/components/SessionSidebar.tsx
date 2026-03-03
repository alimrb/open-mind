import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Session } from '../types';

interface Props {
  workspaceId: string;
  activeSessionId: string | null;
  onSelectSession: (sessionId: string | null) => void;
}

export function SessionSidebar({ workspaceId, activeSessionId, onSelectSession }: Props) {
  const [sessions, setSessions] = useState<Session[]>([]);

  useEffect(() => {
    if (workspaceId) {
      api.listSessions(workspaceId).then(setSessions).catch(() => {});
    }
  }, [workspaceId]);

  return (
    <div className="session-sidebar">
      <h3>Conversations</h3>
      <button onClick={() => onSelectSession(null)} className="btn-sm new-chat">
        + New Chat
      </button>
      <ul>
        {sessions.map((s) => (
          <li
            key={s.id}
            className={s.id === activeSessionId ? 'active' : ''}
            onClick={() => onSelectSession(s.id)}
          >
            <span className="session-title">{s.title || 'Untitled'}</span>
            <span className="session-date">
              {new Date(s.createdAt).toLocaleDateString()}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
