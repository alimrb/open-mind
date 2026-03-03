import { useCallback, useState } from 'react';
import { api } from '../api/client';
import type { KnowledgeUploadResponse } from '../types';

interface Props {
  workspaceId: string;
}

export function FileUpload({ workspaceId }: Props) {
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<KnowledgeUploadResponse | null>(null);
  const [dragOver, setDragOver] = useState(false);

  const upload = useCallback(async (file: File) => {
    setUploading(true);
    setResult(null);
    try {
      const res = await api.uploadKnowledge(workspaceId, file);
      setResult(res);
    } catch (err) {
      console.error('Upload failed:', err);
    } finally {
      setUploading(false);
    }
  }, [workspaceId]);

  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) upload(file);
  }, [upload]);

  return (
    <div className="file-upload">
      <div
        className={`drop-zone ${dragOver ? 'drag-over' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={onDrop}
      >
        {uploading ? (
          <p>Uploading...</p>
        ) : (
          <>
            <p>Drop files here or click to upload</p>
            <input
              type="file"
              onChange={(e) => e.target.files?.[0] && upload(e.target.files[0])}
              accept=".md,.txt,.json,.yaml,.yml,.java,.py,.js,.ts,.tsx"
            />
          </>
        )}
      </div>
      {result && (
        <div className="upload-result">
          Uploaded: {result.filename} — Status: {result.status}
        </div>
      )}
    </div>
  );
}
