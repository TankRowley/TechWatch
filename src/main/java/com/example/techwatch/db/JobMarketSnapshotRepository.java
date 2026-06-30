package com.example.techwatch.db;

import com.example.techwatch.market.JobMarketSnapshot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JobMarketSnapshotRepository {
    private final Database database;
    public JobMarketSnapshotRepository(Database database) { this.database = database; }

    public void save(JobMarketSnapshot value) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO job_market_snapshots(keyword_id,region,source_name,query,job_count,salary_min,
                  salary_max,salary_median,fetched_at,week_start,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(keyword_id,region,source_name,query,week_start) DO UPDATE SET
                  job_count=excluded.job_count,salary_min=excluded.salary_min,salary_max=excluded.salary_max,
                  salary_median=excluded.salary_median,fetched_at=excluded.fetched_at
                """)) {
            statement.setLong(1, value.keywordId()); statement.setString(2, value.region());
            statement.setString(3, value.sourceName()); statement.setString(4, value.query());
            statement.setInt(5, value.jobCount()); setInteger(statement, 6, value.salaryMin());
            setInteger(statement, 7, value.salaryMax()); setInteger(statement, 8, value.salaryMedian());
            statement.setString(9, value.fetchedAt().toString()); statement.setString(10, value.weekStart().toString());
            statement.setString(11, Instant.now().toString()); statement.executeUpdate();
        }
    }

    public List<JobMarketSnapshot> findForWeek(LocalDate weekStart) throws SQLException {
        List<JobMarketSnapshot> values = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM job_market_snapshots WHERE week_start=?")) {
            statement.setString(1, weekStart.toString());
            try (ResultSet result = statement.executeQuery()) { while (result.next()) values.add(map(result)); }
        }
        return values;
    }

    private void setInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.INTEGER); else statement.setInt(index, value);
    }

    private JobMarketSnapshot map(ResultSet result) throws SQLException {
        return new JobMarketSnapshot(result.getLong("keyword_id"), result.getString("region"),
                result.getString("source_name"), result.getString("query"), result.getInt("job_count"),
                integer(result, "salary_min"), integer(result, "salary_max"), integer(result, "salary_median"),
                Instant.parse(result.getString("fetched_at")), LocalDate.parse(result.getString("week_start")));
    }

    private Integer integer(ResultSet result, String name) throws SQLException {
        int value = result.getInt(name); return result.wasNull() ? null : value;
    }
}
