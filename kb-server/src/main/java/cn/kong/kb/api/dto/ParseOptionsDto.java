package cn.kong.kb.api.dto;

import java.util.List;

/**
 * 上传解析方式选项，供前端渲染「解析方式」选择器。
 *
 * @param cloudEnabled        LlamaParse 是否已装配可用（决定 CLOUD 选项是否可选）
 * @param cloudSupportedTypes AUTO 模式下默认会走云端的文档类型（来自 llamaparse.types 配置）
 * @param defaultMode         默认解析方式（AUTO）
 */
public record ParseOptionsDto(
        boolean cloudEnabled,
        List<String> cloudSupportedTypes,
        String defaultMode
) {}
