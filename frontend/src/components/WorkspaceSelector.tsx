import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Workspace } from '../types';

interface Props {
  selected: Workspace | null;
  onSelect: (ws: Workspace) => void;
}

export function WorkspaceSelector({ selected, onSelect }: Props) {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [desc, setDesc] = useState('');

  const load = () => api.listWorkspaces().then(setWorkspaces);

  useEffect(() => { load(); }, []);

  const create = async () => {
    if (!name.trim()) return;
    const ws = await api.createWorkspace(name.trim(), desc.trim() || undefined);
    setWorkspaces((prev) => [...prev, ws]);
    onSelect(ws);
    setShowCreate(false);
    setName('');
    setDesc('');
  };

  return (
    <div className="workspace-selector">
      <label>Workspace</label>
      <select
        value={selected?.id || ''}
        onChange={(e) => {
          const ws = workspaces.find((w) => w.id === e.target.value);
          if (ws) onSelect(ws);
        }}
      >
        <option value="">Select workspace...</option>
        {workspaces.map((ws) => (
          <option key={ws.id} value={ws.id}>{ws.name}</option>
        ))}
      </select>
      <button onClick={() => setShowCreate(!showCreate)} className="btn-sm">
        + New
      </button>
      {showCreate && (
        <div className="create-form">
          <input
            placeholder="Workspace name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && create()}
          />
          <input
            placeholder="Description (optional)"
            value={desc}
            onChange={(e) => setDesc(e.target.value)}
          />
          <button onClick={create} className="btn-primary">Create</button>
        </div>
      )}
    </div>
  );
}
