package cn.kong.kb.ingestion;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 摄入流水线的文档输入契约：指向磁盘上一份<b>已落盘文件</b>的不可变引用。
 *
 * <p><b>为什么不用 {@code MultipartFile}：</b>后者是 Spring Web 的类型，其磁盘临时
 * 文件的生命周期绑定在 HTTP 请求上——请求一结束就被容器删除。而文档摄入是异步的，
 * 若把 {@code MultipartFile} 直接传进异步线程再读取，临时文件往往已不复存在
 * （表现为 {@code NoSuchFileException}）。</p>
 *
 * <p><b>为什么落盘而非驻留内存：</b>web 边界（controller）用
 * {@code MultipartFile#transferTo(Path)} 把上传内容<b>流式复制</b>到一个由本模块自行
 * 创建、生命周期独立的临时文件；{@code DocumentSource} 只持有该文件的 {@link Path}，
 * 不把字节读进堆内存，因此不受文件大小与并发摄入数量的内存放大影响。</p>
 *
 * <p><b>资源所有权：</b>实现 {@link AutoCloseable}，{@link #close()} 删除底层临时文件。
 * 终端消费者（{@code IngestionPipeline}）以 try-with-resources 持有本对象，
 * 无论处理成功或失败都会清理，避免临时文件泄漏。</p>
 *
 * <p><b>架构收益：</b>kb-core 不依赖 Spring Web；将来若要支持本地文件 / URL 等来源，
 * 只需在边界产出指向相应磁盘文件的 {@code DocumentSource}，解析层零改动。</p>
 */
public record DocumentSource(String filename, Path path) implements AutoCloseable {

    public DocumentSource {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename 不能为空");
        }
        if (path == null) {
            throw new IllegalArgumentException("path 不能为 null");
        }
    }

    /** 文件大小（字节），从磁盘实时读取。 */
    public long size() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException("读取文件大小失败：" + filename, e);
        }
    }

    /** 打开底层文件的新输入流；调用方负责关闭。 */
    public InputStream inputStream() throws IOException {
        return Files.newInputStream(path);
    }

    /** 适配需要 Spring {@link Resource} 的解析器（如 Tika），并保留原始文件名。 */
    public Resource toResource() {
        return new FileSystemResource(path) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    /** 删除底层临时文件。由流水线在处理结束后调用（try-with-resources）。 */
    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
