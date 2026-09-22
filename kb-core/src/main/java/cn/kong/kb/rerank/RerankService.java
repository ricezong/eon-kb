package cn.kong.kb.rerank;

import cn.kong.kb.domain.KbChunk;

import java.util.List;

/**
 * 重排服务接口。
 */
public interface RerankService {

    /**
     * 根据查询对切片的相关性进行重排。
     *
     * @param query 用户查询
     * @param candidates 混合检索得到的候选切片
     * @param topN 返回的顶部结果数量
     * @return 重排后的切片列表
     */
    List<KbChunk> rerank(String query, List<KbChunk> candidates, int topN);
}
