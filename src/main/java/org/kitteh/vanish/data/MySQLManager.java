package org.kitteh.vanish.data;

import org.kitteh.vanish.VanishPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class MySQLManager {

    private final VanishPlugin plugin;
    private Connection connection;
    private String host;
    private String port;
    private String database;
    private String username;
    private String password;
    private String tableName;

    public MySQLManager(VanishPlugin plugin) {
        this.plugin = plugin;
        loadCredentials();
        connect();
        createTable();
    }

    private void loadCredentials() {
        this.host = plugin.getConfig().getString("mysql.host", "localhost");
        this.port = plugin.getConfig().getString("mysql.port", "3306");
        this.database = plugin.getConfig().getString("mysql.database", "vanish");
        this.username = plugin.getConfig().getString("mysql.username", "root");
        this.password = plugin.getConfig().getString("mysql.password", "password");
        this.tableName = plugin.getConfig().getString("mysql.table", "vanished_players");
    }

    private void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            synchronized (this) {
                if (connection != null && !connection.isClosed()) {
                    return;
                }
                Class.forName("com.mysql.jdbc.Driver");
                this.connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true&useSSL=false", this.username, this.password);
                plugin.getLogger().info("MySQL connection established.");
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL connection failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("MySQL connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing MySQL connection: " + e.getMessage());
        }
    }

    private void createTable() {
        try (PreparedStatement ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + tableName + " (UUID VARCHAR(36) PRIMARY KEY, Vanished BOOLEAN)")) {
            ps.executeUpdate();
            plugin.getLogger().info("MySQL table '" + tableName + "' checked/created.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating MySQL table: " + e.getMessage());
        }
    }

    public boolean isVanished(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT Vanished FROM " + tableName + " WHERE UUID = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("Vanished");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking vanish status for " + uuid + ": " + e.getMessage());
        }
        return false;
    }

    public void setVanished(UUID uuid, boolean vanished) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO " + tableName + " (UUID, Vanished) VALUES (?, ?) ON DUPLICATE KEY UPDATE Vanished = ?")) {
            ps.setString(1, uuid.toString());
            ps.setBoolean(2, vanished);
            ps.setBoolean(3, vanished);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting vanish status for " + uuid + ": " + e.getMessage());
        }
    }

    public void removePlayer(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM " + tableName + " WHERE UUID = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing player " + uuid + " from database: " + e.getMessage());
        }
    }
}
