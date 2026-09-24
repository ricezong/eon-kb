-- ============================================================
-- Eon 知识库 - Docker 初始化脚本
-- 由 docker-entrypoint 在首次建库后自动对 eon-kb 执行。
--
-- 与 ../schema.sql 的唯一区别：去掉了 CREATE DATABASE 语句
-- （库已由 compose 的 POSTGRES_DB=eon-kb 创建，脚本直接连到该库执行）。
-- 如需修改表结构，请同步更新 schema.sql 与本文件。
-- ============================================================

-- ========================= 扩展 =========================
-- 依赖镜像已内置：vector(pgvector) / pg_trgm / zhparser

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS zhparser;

-- 创建中文全文检索配置
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese') THEN
        CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
        ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
    END IF;
END $$;

-- ========================= 表 =========================

-- 文档表
CREATE TABLE IF NOT EXISTS documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name     VARCHAR(500) NOT NULL,
    file_type     VARCHAR(20)  NOT NULL,
    file_size     BIGINT,
    original_path VARCHAR(1000),
    chunk_count   INT DEFAULT 0,
    status        VARCHAR(20) DEFAULT 'PROCESSING',
    parse_mode    VARCHAR(20) DEFAULT 'AUTO',
    error_message TEXT,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW()
);

-- 切片表（兼容 Spring AI PgVectorStore）
CREATE TABLE IF NOT EXISTS kb_chunks (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content        TEXT NOT NULL,
    metadata       JSONB DEFAULT '{}',
    embedding      vector(1024),
    document_id    UUID REFERENCES documents(id) ON DELETE CASCADE,
    search_content tsvector,
    chunk_index    INT,
    chunk_type     VARCHAR(50),
    title          VARCHAR(500),
    created_at     TIMESTAMP DEFAULT NOW()
);

-- 知识库分类表
CREATE TABLE IF NOT EXISTS knowledge_bases (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    description TEXT DEFAULT '',
    color       VARCHAR(7) DEFAULT '#3b82f6',
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- 文档-知识库 多对多关联表
CREATE TABLE IF NOT EXISTS document_knowledge_bases (
    document_id       UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    created_at        TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (document_id, knowledge_base_id)
);

-- ========================= 索引 =========================

-- 切片表：HNSW 向量索引（余弦距离）
CREATE INDEX IF NOT EXISTS idx_chunks_embedding
    ON kb_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 切片表：中文全文检索 GIN 索引
CREATE INDEX IF NOT EXISTS idx_chunks_search
    ON kb_chunks USING gin (search_content);

-- 切片表：模糊匹配三元组 GIN 索引
CREATE INDEX IF NOT EXISTS idx_chunks_trgm
    ON kb_chunks USING gin (content gin_trgm_ops);

-- 切片表：文档外键索引
CREATE INDEX IF NOT EXISTS idx_chunks_doc_id
    ON kb_chunks (document_id);

-- 知识库表：名称唯一约束
CREATE UNIQUE INDEX IF NOT EXISTS idx_kb_name ON knowledge_bases (name);

-- 关联表：双向索引（按知识库查文档 + 按文档查知识库）
CREATE INDEX IF NOT EXISTS idx_dkb_doc ON document_knowledge_bases (document_id);
CREATE INDEX IF NOT EXISTS idx_dkb_kb  ON document_knowledge_bases (knowledge_base_id);

-- ========================= 触发器 =========================

-- 内容变更时自动更新 tsvector
CREATE OR REPLACE FUNCTION update_search_content()
RETURNS trigger AS $$
BEGIN
    NEW.search_content := to_tsvector('chinese', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_search_content ON kb_chunks;
CREATE TRIGGER trg_update_search_content
    BEFORE INSERT OR UPDATE OF content ON kb_chunks
    FOR EACH ROW EXECUTE FUNCTION update_search_content();
