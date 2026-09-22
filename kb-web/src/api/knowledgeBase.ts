import { http, unwrap } from './client';
import type { KnowledgeBase, KnowledgeBaseForm, Document } from './types';

/** 知识库 API 封装 */
export const knowledgeBaseApi = {
  /** GET /api/knowledge-bases */
  list(): Promise<KnowledgeBase[]> {
    return unwrap(http.get<KnowledgeBase[]>('/knowledge-bases'));
  },

  /** GET /api/knowledge-bases/{id} */
  get(id: string): Promise<KnowledgeBase> {
    return unwrap(http.get<KnowledgeBase>(`/knowledge-bases/${id}`));
  },

  /** POST /api/knowledge-bases */
  create(payload: KnowledgeBaseForm): Promise<KnowledgeBase> {
    return unwrap(http.post<KnowledgeBase>('/knowledge-bases', payload));
  },

  /** PUT /api/knowledge-bases/{id} —— null 字段表示保持原值 */
  update(id: string, payload: Partial<KnowledgeBaseForm>): Promise<KnowledgeBase> {
    return unwrap(http.put<KnowledgeBase>(`/knowledge-bases/${id}`, payload));
  },

  /** DELETE /api/knowledge-bases/{id} */
  remove(id: string): Promise<{ message: string }> {
    return unwrap(http.delete<{ message: string }>(`/knowledge-bases/${id}`));
  },

  /** GET /api/knowledge-bases/{id}/documents */
  documents(id: string): Promise<Document[]> {
    return unwrap(http.get<Document[]>(`/knowledge-bases/${id}/documents`));
  }
};
