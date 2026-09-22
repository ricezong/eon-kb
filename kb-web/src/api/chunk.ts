import { http, unwrap } from './client';
import type { Chunk } from './types';

/** 切片 API 封装 */
export const chunkApi = {
  /** GET /api/chunks?documentId=xxx */
  listByDocument(documentId: string): Promise<Chunk[]> {
    return unwrap(
      http.get<Chunk[]>('/chunks', { params: { documentId } })
    );
  },

  /** GET /api/chunks/{id} —— 含 documentName 与 metadata 的完整视图 */
  get(id: string): Promise<Chunk> {
    return unwrap(http.get<Chunk>(`/chunks/${id}`));
  }
};
