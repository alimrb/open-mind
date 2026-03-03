export interface Workspace {
  id: string;
  name: string;
  description: string | null;
  directoryPath: string;
  createdAt: string;
}

export interface Session {
  id: string;
  title: string;
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  confidence: number | null;
  citations: Citation[];
  createdAt: string;
}

export interface Citation {
  chunkId: string;
  sourceFile: string;
  chunkIndex: number;
  snippet: string;
  score: number;
}

export interface ChatResponse {
  messageId: string;
  sessionId: string;
  content: string;
  confidence: number | null;
  citations: Citation[];
  timestamp: string;
}

export interface StreamEvent {
  type: string;
  content: string;
  role: string;
}

export interface HealthResponse {
  status: string;
  components: Record<string, { status: string; details: string }>;
}

export interface KnowledgeUploadResponse {
  fileId: string;
  filename: string;
  status: string;
  chunkCount: number;
}
