import axios from 'axios';
import type { AskRequest, AskResponse } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: `${API_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const askAssistant = async (request: AskRequest): Promise<AskResponse> => {
  const response = await api.post<AskResponse>('/ask', request);
  return response.data;
};

export const healthCheck = async (): Promise<{ status: string }> => {
  const response = await api.get('/health');
  return response.data;
};
