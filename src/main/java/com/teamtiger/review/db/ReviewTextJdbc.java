package com.teamtiger.review.db;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

@Repository
@RequiredArgsConstructor
public class ReviewTextJdbc {

    private final JdbcTemplate jdbc;

    /**
     * review_text INSERT (user_id 포함)
     */
    public long insert(long userId, String content, String s3Key, String contentType, Long sizeBytes) {
        final String sql =
                "INSERT INTO review_text(user_id, content, s3_key, content_type, size_bytes) VALUES (?, ?, ?, ?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, content);
            if (s3Key == null) ps.setNull(3, Types.VARCHAR); else ps.setString(3, s3Key);
            if (contentType == null) ps.setNull(4, Types.VARCHAR); else ps.setString(4, contentType);
            if (sizeBytes == null) ps.setNull(5, Types.BIGINT); else ps.setLong(5, sizeBytes);
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }
}
