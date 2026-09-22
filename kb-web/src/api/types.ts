/**
 * 与后端 DTO 严格对应的类型定义。
 * 参考：kb-server/src/main/java/cn/kong/kb/api/dto/*.java
 */

/** 文档处理状态（对应 DocumentStatus 枚举） */
export type DocumentStatus =
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'PENDING';

/** 文档类型（对应 KbDocumentType 枚举，白名单） */
export type DocumentFileType =
  | 'PDF'
  | 'DOCX'
  | 'DOC'
  | 'PPTX'
  | 'PPT'
  | 'XLSX'
  | 'XLS'
  | 'TXT'
  | 'MD'
  | 'HTML'
  | string;

/** 知识库 */
export interface KnowledgeBase {
  id: string | null;
  name: string;
  description: string;
  color: string;
  documentCount: number;
  createdAt: string | null;
}

/** 文档 */
export interface Document {
  id: string;
  fileName: string;
  fileType: DocumentFileType;
  fileSize: number | null;
  chunkCount: number;
  status: DocumentStatus | null;
  errorMessage: string | null;
  createdAt: string | null;
  knowledgeBases: KnowledgeBase[];
}

/** 切片（含预览用元数据） */
export interface Chunk {
  id: string;
  content: string;
  documentId: string;
  documentName?: string | null;
  chunkIndex: number;
  chunkType: string;
  title: string;
  metadata?: Record<string, unknown> | null;
}

/** 聊天引用 */
export interface Citation {
  id: string;
  index: number;
  documentName: string;
  chunkType: string;
  title: string;
  preview: string;
  score: number;
}

/** 聊天请求 */
export interface ChatRequestPayload {
  question: string;
  /** 为空或 null 表示全局搜索 */
  knowledgeBaseIds?: string[] | null;
}

/** 聊天响应（同步） */
export interface ChatResponsePayload {
  answer: string;
  citations: Citation[];
}

/** 通用错误响应体（后端 GlobalExceptionHandler 约定） */
export interface ApiErrorBody {
  error: string;
}

/** 创建 / 更新知识库请求 */
export interface KnowledgeBaseForm {
  name: string;
  description?: string;
  color?: string;
}

/** 上传文档响应 */
export interface UploadResponse {
  documentId: string;
  fileName: string;
  status: DocumentStatus;
  message: string;
}
