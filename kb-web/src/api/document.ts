import { http, unwrap } from './client';
import type { Document, KnowledgeBase, ParseMode, ParseOptions, UploadResponse } from './types';

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

  /** GET /api/documents/parse-options —— 查询云端解析能力，用于上传时决定选项 */
  parseOptions(): Promise<ParseOptions> {
    return unwrap(http.get<ParseOptions>('/documents/parse-options'));
  },

  /**
   * POST /api/documents/upload (multipart/form-data)
   * @param file        要上传的文件（PDF/Word/PPT/Excel/TXT/MD/HTML，最大 100MB）
   * @param kbIds       可选：上传后立即归属的知识库 ID 列表
   * @param parseMode   解析方式（AUTO/LOCAL/CLOUD），缺省 AUTO
   * @param onProgress  上传进度回调（0-100）
   */
  upload(
    file: File,
    kbIds?: string[],
    parseMode: ParseMode = 'AUTO',
    onProgress?: (percent: number) => void
  ): Promise<UploadResponse> {
    const form = new FormData();
    form.append('file', file);
    form.append('parseMode', parseMode);
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
