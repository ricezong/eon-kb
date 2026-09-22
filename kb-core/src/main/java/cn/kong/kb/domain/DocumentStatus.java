package cn.kong.kb.domain;

/**
 * 文档处理状态。
 *
 * <p>与 documents.status 列的取值一一对应（MyBatis 默认按枚举名映射），
 * 取代散落各处的字符串字面量。</p>
 */
public enum DocumentStatus {
    /** 已入库，摄入流水线处理中 */
    PROCESSING,
    /** 解析、分块、润色、向量化全部完成 */
    COMPLETED,
    /** 处理失败，原因记录在 error_message 列 */
    FAILED
}
