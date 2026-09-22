package cn.kong.kb.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * MyBatis TypeHandler，用于 java.util.UUID 与 PostgreSQL UUID 列之间的转换。
 *
 * <p>MyBatis 3.5.x 没有内置的 UUIDTypeHandler，因此此处自行提供。
 * PostgreSQL JDBC 驱动会通过 {@code rs.getObject()} 原生返回
 * {@code java.util.UUID}，所以本 Handler 主要负责写入端的桥接。</p>
 */
@MappedTypes(UUID.class)
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                     UUID parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return toUuid(value);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex);
        return toUuid(value);
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = cs.getObject(columnIndex);
        return toUuid(value);
    }

    /**
     * 将 JDBC 取出的值转换为 UUID。PostgreSQL 会直接返回 UUID，
     * 但某些驱动或配置可能返回其字符串表示形式。
     */
    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        String str = value.toString();
        if (str.isBlank()) {
            return null;
        }
        return UUID.fromString(str);
    }
}
