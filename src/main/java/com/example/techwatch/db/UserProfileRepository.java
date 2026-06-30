package com.example.techwatch.db;

import com.example.techwatch.profile.UserProfile;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class UserProfileRepository {
    private final Database database;

    public UserProfileRepository(Database database) { this.database = database; }

    public Optional<UserProfile> find() throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM user_profile ORDER BY id LIMIT 1"); ResultSet result = statement.executeQuery()) {
            if (!result.next()) return Optional.empty();
            return Optional.of(new UserProfile(result.getLong("id"), result.getString("display_name"),
                    result.getString("primary_goal"), result.getString("experience_level")));
        }
    }

    public void save(UserProfile profile) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect()) {
            Long existingId = null;
            try (PreparedStatement find = connection.prepareStatement("SELECT id FROM user_profile ORDER BY id LIMIT 1");
                 ResultSet result = find.executeQuery()) {
                if (result.next()) existingId = result.getLong(1);
            }
            if (existingId == null) {
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO user_profile(display_name,primary_goal,experience_level,created_at,updated_at)
                        VALUES(?,?,?,?,?)
                        """)) {
                    insert.setString(1, profile.displayName()); insert.setString(2, profile.primaryGoal());
                    insert.setString(3, profile.experienceLevel()); insert.setString(4, now); insert.setString(5, now);
                    insert.executeUpdate();
                }
            } else {
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE user_profile SET display_name=?,primary_goal=?,experience_level=?,updated_at=? WHERE id=?
                        """)) {
                    update.setString(1, profile.displayName()); update.setString(2, profile.primaryGoal());
                    update.setString(3, profile.experienceLevel()); update.setString(4, now); update.setLong(5, existingId);
                    update.executeUpdate();
                }
            }
        }
    }

    public void saveInterests(Set<String> categories) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement disable = connection.prepareStatement("UPDATE user_interest_categories SET enabled=0,updated_at=?")) {
                    disable.setString(1, now); disable.executeUpdate();
                }
                try (PreparedStatement upsert = connection.prepareStatement("""
                        INSERT INTO user_interest_categories(category,enabled,created_at,updated_at) VALUES(?,1,?,?)
                        ON CONFLICT(category) DO UPDATE SET enabled=1,updated_at=excluded.updated_at
                        """)) {
                    for (String category : categories) {
                        upsert.setString(1, category); upsert.setString(2, now); upsert.setString(3, now); upsert.addBatch();
                    }
                    upsert.executeBatch();
                }
                connection.commit();
            } catch (SQLException error) { connection.rollback(); throw error; }
            finally { connection.setAutoCommit(true); }
        }
    }

    public Set<String> findEnabledInterests() throws SQLException {
        Set<String> categories = new LinkedHashSet<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT category FROM user_interest_categories WHERE enabled=1 ORDER BY category");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) categories.add(result.getString(1));
        }
        return categories;
    }
}
