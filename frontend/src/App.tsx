import { useState } from 'react';
import { WorkspaceSelector } from './components/WorkspaceSelector';
import { SessionSidebar } from './components/SessionSidebar';
import { ChatView } from './components/ChatView';
import { FileUpload } from './components/FileUpload';
import type { Workspace } from './types';

function App() {
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [showUpload, setShowUpload] = useState(false);

  return (
    <div className="app">
      <header className="app-header">
        <h1>HAGAP</h1>
        <span className="subtitle">Hummingbird AI Gateway & Assistant</span>
        <WorkspaceSelector selected={workspace} onSelect={(ws) => { setWorkspace(ws); setSessionId(null); }} />
      </header>

      <div className="app-body">
        {workspace && (
          <SessionSidebar
            workspaceId={workspace.id}
            activeSessionId={sessionId}
            onSelectSession={setSessionId}
          />
        )}

        <main className="app-main">
          {workspace ? (
            <>
              <div className="main-toolbar">
                <button onClick={() => setShowUpload(!showUpload)} className="btn-sm">
                  {showUpload ? 'Hide Upload' : 'Upload Knowledge'}
                </button>
              </div>
              {showUpload && <FileUpload workspaceId={workspace.id} />}
              <ChatView
                workspaceId={workspace.id}
                sessionId={sessionId}
                onSessionCreated={setSessionId}
              />
            </>
          ) : (
            <div className="empty-state">
              <h2>Welcome to HAGAP</h2>
              <p>Select or create a workspace to get started.</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default App;
