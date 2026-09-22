import { http, unwrap } from './client';
import type { Document, KnowledgeBase, UploadResponse } from './types';

/** 文档 API 封装 */
export const documentApi = {
  /** GET /api/documents */
  list(): Promise<Document[]> {
    return unwrap(http.get<Document[]>('/documents'));
  },

  /** GET /api/documents/{id} */
  get(id: string): Promise<Document> {
    return unwrap(http.get<Document>(`/documents/${id}`));
  },

  /** DELETE /api/documents/{id} */
  remove(id: string): Promise<{ message: string }> {
    return unwrap(http.delete<{ message: string }>(`/documents/${id}`));
  },

  /**
   * POST /api/documents/upload (multipart/form-data)
   * @param file        要上传的文件（PDF/Word/PPT/Excel/TXT/MD/HTML，最大 100MB）
   * @param kbIds       可选：上传后立即归属的知识库 ID 列表
   * @param onProgress  上传进度回调（0-100）
   */
  upload(
    file: File,
    kbIds?: string[],
    onProgress?: (percent: number) => void
  ): Promise<UploadResponse> {
    const form = new FormData();
    form.append('file', file);
    (kbIds || []).forEach((id) => form.append('knowledgeBaseIds', id));
    return unwrap(
      http.post<UploadResponse>('/documents/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        // 大文件上传超时放宽到 10 分钟
        timeout: 600_000,
        onUploadProgress: (evt) => {
          if (onProgress && evt.total) {
            onProgress(Math.round((evt.loaded * 100) / evt.total));
          }
        }
      })
    );
  },

  /** GET /api/documents/{id}/knowledge-bases */
  knowledgeBases(id: string): Promise<KnowledgeBase[]> {
    return unwrap(http.get<KnowledgeBase[]>(`/documents/${id}/knowledge-bases`));
  },

  /** PUT /api/documents/{id}/knowledge-bases —— 全量替换归属 */
  assignKnowledgeBases(
    id: string,
    knowledgeBaseIds: string[]
  ): Promise<{ message: string }> {
    return unwrap(
      http.put<{ message: string }>(`/documents/${id}/knowledge-bases`, {
        knowledgeBaseIds
      })
    );
  }
};
