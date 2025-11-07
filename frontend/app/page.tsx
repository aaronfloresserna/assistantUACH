'use client';

import { useState } from 'react';
import { askAssistant } from '@/lib/api';
import type { Message } from '@/types';
import ReactMarkdown from 'react-markdown';

export default function Home() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [materia, setMateria] = useState('');
  const [semesterLevel, setSemesterLevel] = useState<number | undefined>();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!input.trim() || loading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input,
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      const response = await askAssistant({
        question: input,
        materia: materia || undefined,
        semesterLevel,
        topK: 5,
      });

      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.answer,
        sources: response.sources,
        metadata: response.metadata,
        timestamp: new Date(),
      };

      setMessages(prev => [...prev, assistantMessage]);
    } catch (error) {
      console.error('Error:', error);
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: 'Lo siento, ocurrió un error al procesar tu pregunta. Por favor, intenta de nuevo.',
        timestamp: new Date(),
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200 px-4 py-4">
        <div className="max-w-5xl mx-auto">
          <h1 className="text-2xl font-bold text-gray-900">Luis Amigo</h1>
          <p className="text-sm text-gray-600">Asistente Jurídico UACH</p>
        </div>
      </header>

      {/* Filters */}
      <div className="bg-white border-b border-gray-200 px-4 py-3">
        <div className="max-w-5xl mx-auto flex gap-4">
          <select
            value={materia}
            onChange={(e) => setMateria(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">Todas las materias</option>
            <option value="constitucional">Constitucional</option>
            <option value="civil">Civil</option>
            <option value="penal">Penal</option>
            <option value="mercantil">Mercantil</option>
            <option value="laboral">Laboral</option>
            <option value="administrativo">Administrativo</option>
          </select>

          <select
            value={semesterLevel || ''}
            onChange={(e) => setSemesterLevel(e.target.value ? Number(e.target.value) : undefined)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">Todos los semestres</option>
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((sem) => (
              <option key={sem} value={sem}>
                {sem}° Semestre
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-6">
        <div className="max-w-5xl mx-auto space-y-6">
          {messages.length === 0 && (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">⚖️</div>
              <h2 className="text-2xl font-semibold text-gray-700 mb-2">
                ¡Hola! Soy Luis Amigo
              </h2>
              <p className="text-gray-600 max-w-md mx-auto">
                Tu asistente jurídico académico. Pregúntame sobre leyes, artículos constitucionales,
                conceptos jurídicos y más. Estoy aquí para ayudarte con tus estudios.
              </p>
            </div>
          )}

          {messages.map((message) => (
            <div
              key={message.id}
              className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-3xl rounded-lg px-4 py-3 ${
                  message.role === 'user'
                    ? 'bg-primary-600 text-white'
                    : 'bg-white border border-gray-200 shadow-sm'
                }`}
              >
                <div className={message.role === 'assistant' ? 'prose prose-sm max-w-none' : ''}>
                  {message.role === 'assistant' ? (
                    <ReactMarkdown>{message.content}</ReactMarkdown>
                  ) : (
                    <p>{message.content}</p>
                  )}
                </div>

                {message.sources && message.sources.length > 0 && (
                  <div className="mt-4 pt-4 border-t border-gray-200">
                    <p className="text-sm font-semibold text-gray-700 mb-2">Fuentes consultadas:</p>
                    <div className="space-y-2">
                      {message.sources.map((source, idx) => (
                        <div key={idx} className="text-sm bg-gray-50 rounded p-2">
                          <div className="flex justify-between items-start mb-1">
                            <span className="font-medium text-gray-900">
                              {source.lawReference || `Documento ${source.documentId}`}
                            </span>
                            <span className="text-xs text-gray-500">
                              {(source.similarityScore * 100).toFixed(1)}% relevancia
                            </span>
                          </div>
                          {source.text && source.text.length > 150 ? (
                            <p className="text-gray-600 text-xs">
                              {source.text.substring(0, 150)}...
                            </p>
                          ) : (
                            <p className="text-gray-600 text-xs">{source.text}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {message.metadata && (
                  <div className="mt-2 text-xs text-gray-500">
                    {message.metadata.documentsRetrieved} documentos consultados •
                    {message.metadata.processingTimeMs}ms
                  </div>
                )}
              </div>
            </div>
          ))}

          {loading && (
            <div className="flex justify-start">
              <div className="bg-white border border-gray-200 rounded-lg px-4 py-3 shadow-sm">
                <div className="flex items-center space-x-2">
                  <div className="animate-pulse flex space-x-1">
                    <div className="h-2 w-2 bg-gray-400 rounded-full"></div>
                    <div className="h-2 w-2 bg-gray-400 rounded-full animation-delay-200"></div>
                    <div className="h-2 w-2 bg-gray-400 rounded-full animation-delay-400"></div>
                  </div>
                  <span className="text-sm text-gray-600">Pensando...</span>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Input */}
      <div className="border-t border-gray-200 bg-white px-4 py-4">
        <form onSubmit={handleSubmit} className="max-w-5xl mx-auto">
          <div className="flex gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Escribe tu pregunta jurídica..."
              className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              disabled={loading}
            />
            <button
              type="submit"
              disabled={loading || !input.trim()}
              className="px-6 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed font-medium"
            >
              Enviar
            </button>
          </div>
          <p className="text-xs text-gray-500 mt-2">
            Esto es material académico y no constituye asesoría jurídica profesional.
          </p>
        </form>
      </div>
    </div>
  );
}
