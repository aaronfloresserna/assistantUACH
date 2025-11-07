export interface AskRequest {
  question: string;
  materia?: string;
  semesterLevel?: number;
  topK?: number;
}

export interface SourceReference {
  documentId: number;
  text: string;
  lawReference: string | null;
  source: string;
  similarityScore: number;
}

export interface ResponseMetadata {
  documentsRetrieved: number;
  materia: string | null;
  timestamp: string;
  processingTimeMs: number;
}

export interface AskResponse {
  answer: string;
  sources: SourceReference[];
  metadata: ResponseMetadata;
  disclaimer: string;
}

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  sources?: SourceReference[];
  metadata?: ResponseMetadata;
  timestamp: Date;
}
