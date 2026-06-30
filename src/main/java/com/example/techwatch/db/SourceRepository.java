package com.example.techwatch.db;

import com.example.techwatch.source.Source;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SourceRepository {
    private final Database database;

    public SourceRepository(Database database) { this.database = database; }

    public Source save(Source source) throws SQLException {
        String now = Instant.now().toString();
        String sql = """
                INSERT INTO sources(name,url,type,trust_score,status,created_at,updated_at)
                VALUES(?,?,?,?,?,?,?)
                ON CONFLICT(url) DO UPDATE SET name=excluded.name,type=excluded.type,
                  trust_score=excluded.trust_score,status=excluded.status,updated_at=excluded.updated_at
                """;
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source.name());
            statement.setString(2, source.url());
            statement.setString(3, source.type());
            statement.setInt(4, source.trustScore());
            statement.setString(5, source.status());
            statement.setString(6, now);
            statement.setString(7, now);
            statement.executeUpdate();
        }
        return findByUrl(source.url());
    }

    public Source findByUrl(String url) throws SQLException {
        try (var connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM sources WHERE url=?")) {
            statement.setString(1, url);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("保存した情報源を取得できません: " + url);
                return map(result);
            }
        }
    }

    public List<Source> findAll() throws SQLException {
        List<Source> sources = new ArrayList<>();
        try (var connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM sources ORDER BY name");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) sources.add(map(result));
        }
        return sources;
    }

    private Source map(ResultSet result) throws SQLException {
        return new Source(result.getLong("id"), result.getString("name"), result.getString("url"),
                result.getString("type"), result.getInt("trust_score"), result.getString("status"));
    }
}
