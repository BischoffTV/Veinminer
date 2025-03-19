package org.bischofftv.veinminer.commands;

import org.bischofftv.veinminer.Veinminer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCommand implements CommandExecutor {

    private final Veinminer plugin;

    public AdminCommand(Veinminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Überprüfe, ob der Sender die Berechtigung hat
        if (!sender.hasPermission("veinminer.admin")) {
            sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "check":
                return checkDatabaseConnection(sender);
            case "mysql":
                if (args.length > 1 && args[1].equalsIgnoreCase("reload")) {
                    return reloadMySQLConnection(sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unbekannter MySQL-Befehl. Verwende: /veinminer mysql reload");
                }
                break;
            case "debug":
                if (args.length > 1) {
                    return toggleDebugMode(sender, args[1].equalsIgnoreCase("on"));
                } else {
                    boolean currentDebug = plugin.getConfig().getBoolean("settings.debug", false);
                    sender.sendMessage(ChatColor.YELLOW + "Debug-Modus ist aktuell: " +
                            (currentDebug ? ChatColor.GREEN + "AN" : ChatColor.RED + "AUS"));
                    sender.sendMessage(ChatColor.YELLOW + "Verwende: /veinminer debug on|off");
                }
                break;
            case "config":
                if (args.length >= 3) {
                    return handleConfigCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                } else {
                    sender.sendMessage(ChatColor.RED + "Verwendung: /veinminer config <pfad> <wert>");
                }
                break;
            case "sync":
                return forceSyncData(sender);
            case "reload":
                return reloadPlugin(sender);
            case "help":
                showHelp(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unbekannter Befehl. Verwende /veinminer help für Hilfe.");
                break;
        }

        return true;
    }

    private boolean checkDatabaseConnection(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Überprüfe Datenbankverbindung...");

        if (plugin.getDatabaseManager().isFallbackMode()) {
            sender.sendMessage(ChatColor.RED + "Plugin läuft im Fallback-Modus. MySQL-Verbindung ist nicht aktiv.");
            sender.sendMessage(ChatColor.YELLOW + "Daten werden lokal gespeichert.");
            return true;
        }

        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                sender.sendMessage(ChatColor.RED + "Konnte keine Verbindung zur Datenbank herstellen.");
                return true;
            }

            boolean isValid = conn.isValid(5); // 5 Sekunden Timeout
            conn.close();

            if (isValid) {
                sender.sendMessage(ChatColor.GREEN + "Datenbankverbindung ist aktiv und funktioniert.");

                // Zeige Datenbankeinstellungen
                FileConfiguration config = plugin.getConfig();
                String host = config.getString("database.host", "localhost");
                int port = config.getInt("database.port", 3306);
                String database = config.getString("database.database",
                        config.getString("database.name", "veinminer"));
                String username = config.getString("database.username", "root");
                String tablePrefix = config.getString("database.table-prefix", "vm_");

                sender.sendMessage(ChatColor.YELLOW + "Datenbankeinstellungen:");
                sender.sendMessage(ChatColor.YELLOW + "- Host: " + ChatColor.WHITE + host + ":" + port);
                sender.sendMessage(ChatColor.YELLOW + "- Datenbank: " + ChatColor.WHITE + database);
                sender.sendMessage(ChatColor.YELLOW + "- Benutzer: " + ChatColor.WHITE + username);
                sender.sendMessage(ChatColor.YELLOW + "- Tabellenpräfix: " + ChatColor.WHITE + tablePrefix);
            } else {
                sender.sendMessage(ChatColor.RED + "Datenbankverbindung ist nicht gültig.");
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Fehler bei der Überprüfung der Datenbankverbindung: " + e.getMessage());
            plugin.getLogger().severe("Fehler bei der Überprüfung der Datenbankverbindung: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean reloadMySQLConnection(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Lade MySQL-Verbindung neu...");

        try {
            boolean wasInFallbackMode = plugin.getDatabaseManager().isFallbackMode();

            // Versuche, die Verbindung neu zu laden
            plugin.getDatabaseManager().reloadConnection();

            // Überprüfe, ob die Verbindung erfolgreich hergestellt wurde
            if (plugin.getDatabaseManager().isFallbackMode()) {
                if (wasInFallbackMode) {
                    sender.sendMessage(ChatColor.RED + "Konnte keine Verbindung zur Datenbank herstellen. Plugin bleibt im Fallback-Modus.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Verbindung zur Datenbank verloren. Plugin ist jetzt im Fallback-Modus.");
                }
            } else {
                if (wasInFallbackMode) {
                    sender.sendMessage(ChatColor.GREEN + "Verbindung zur Datenbank wiederhergestellt! Plugin ist nicht mehr im Fallback-Modus.");
                    sender.sendMessage(ChatColor.YELLOW + "Lokale Daten werden mit der Datenbank synchronisiert...");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "MySQL-Verbindung erfolgreich neu geladen.");
                }
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Fehler beim Neuladen der MySQL-Verbindung: " + e.getMessage());
            plugin.getLogger().severe("Fehler beim Neuladen der MySQL-Verbindung: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean toggleDebugMode(CommandSender sender, boolean enable) {
        // Aktuellen Wert abrufen
        boolean currentDebug = plugin.getConfig().getBoolean("settings.debug", false);

        // Wenn der Wert bereits dem gewünschten Wert entspricht, nichts tun
        if (currentDebug == enable) {
            sender.sendMessage(ChatColor.YELLOW + "Debug-Modus ist bereits " +
                    (enable ? ChatColor.GREEN + "aktiviert" : ChatColor.RED + "deaktiviert") + ChatColor.YELLOW + ".");
            return true;
        }

        // Wert in der Konfiguration ändern
        plugin.getConfig().set("settings.debug", enable);
        plugin.saveConfig();

        // Debug-Modus im Plugin aktualisieren
        plugin.setDebugMode(enable);

        sender.sendMessage(ChatColor.YELLOW + "Debug-Modus wurde " +
                (enable ? ChatColor.GREEN + "aktiviert" : ChatColor.RED + "deaktiviert") + ChatColor.YELLOW + ".");

        return true;
    }

    private boolean handleConfigCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /veinminer config <pfad> <wert>");
            return true;
        }

        String path = args[0];
        String valueStr = args[1];

        // Überprüfe, ob der Pfad existiert
        if (!plugin.getConfig().contains(path)) {
            sender.sendMessage(ChatColor.RED + "Der Konfigurationspfad '" + path + "' existiert nicht.");
            return true;
        }

        // Bestimme den Typ des aktuellen Werts
        Object currentValue = plugin.getConfig().get(path);

        try {
            // Setze den neuen Wert basierend auf dem Typ des aktuellen Werts
            if (currentValue instanceof Boolean) {
                boolean value = Boolean.parseBoolean(valueStr);
                plugin.getConfig().set(path, value);
                sender.sendMessage(ChatColor.GREEN + "Konfigurationswert '" + path + "' auf " + value + " gesetzt.");
            } else if (currentValue instanceof Integer) {
                int value = Integer.parseInt(valueStr);
                plugin.getConfig().set(path, value);
                sender.sendMessage(ChatColor.GREEN + "Konfigurationswert '" + path + "' auf " + value + " gesetzt.");
            } else if (currentValue instanceof Double) {
                double value = Double.parseDouble(valueStr);
                plugin.getConfig().set(path, value);
                sender.sendMessage(ChatColor.GREEN + "Konfigurationswert '" + path + "' auf " + value + " gesetzt.");
            } else if (currentValue instanceof Long) {
                long value = Long.parseLong(valueStr);
                plugin.getConfig().set(path, value);
                sender.sendMessage(ChatColor.GREEN + "Konfigurationswert '" + path + "' auf " + value + " gesetzt.");
            } else if (currentValue instanceof String) {
                // Bei Strings mehrere Argumente zu einem String zusammenfügen
                StringBuilder value = new StringBuilder(valueStr);
                for (int i = 2; i < args.length; i++) {
                    value.append(" ").append(args[i]);
                }
                plugin.getConfig().set(path, value.toString());
                sender.sendMessage(ChatColor.GREEN + "Konfigurationswert '" + path + "' auf '" + value + "' gesetzt.");
            } else if (currentValue instanceof List) {
                // Listen werden nicht unterstützt
                sender.sendMessage(ChatColor.RED + "Listen können nicht über diesen Befehl geändert werden.");
                return true;
            } else {
                // Unbekannter Typ
                sender.sendMessage(ChatColor.RED + "Der Typ des Konfigurationswerts wird nicht unterstützt.");
                return true;
            }

            // Speichere die Konfiguration
            plugin.saveConfig();

            // Lade die Konfiguration neu, wenn nötig
            if (path.startsWith("settings.") || path.equals("settings")) {
                plugin.getConfigManager().reloadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Konfiguration neu geladen.");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Ungültiger Wert für den Typ des Konfigurationswerts.");
            return true;
        }

        return true;
    }

    private boolean forceSyncData(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Erzwinge Datensynchronisierung...");

        if (plugin.getDatabaseManager().isFallbackMode()) {
            sender.sendMessage(ChatColor.RED + "Plugin läuft im Fallback-Modus. Synchronisierung nicht möglich.");
            return true;
        }

        try {
            plugin.forceSyncDataNow();
            sender.sendMessage(ChatColor.GREEN + "Datensynchronisierung wurde erzwungen.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Fehler bei der Datensynchronisierung: " + e.getMessage());
            plugin.getLogger().severe("Fehler bei der Datensynchronisierung: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean reloadPlugin(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Lade Plugin neu...");

        try {
            // Speichere alle Daten
            plugin.getPlayerDataManager().saveAllData();

            if (plugin.getAchievementManager().isEnabled()) {
                plugin.getAchievementManager().saveAllAchievements();
            }

            // Lade Konfiguration neu
            plugin.getConfigManager().reloadConfig();
            plugin.getMessageManager().reloadMessages();

            // Lade Achievements neu
            if (plugin.getAchievementManager().isEnabled()) {
                plugin.getAchievementManager().loadSettings();
            }

            // Lade Level-Einstellungen neu
            plugin.getLevelManager().reloadLevelSettings();

            // Starte Auto-Save-Task neu
            plugin.restartAutoSaveTask();

            sender.sendMessage(ChatColor.GREEN + "Plugin wurde neu geladen.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Fehler beim Neuladen des Plugins: " + e.getMessage());
            plugin.getLogger().severe("Fehler beim Neuladen des Plugins: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== VeinMiner Admin-Befehle ===");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer check " + ChatColor.WHITE + "- Überprüft die Datenbankverbindung");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer mysql reload " + ChatColor.WHITE + "- Lädt die MySQL-Verbindung neu");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer debug on|off " + ChatColor.WHITE + "- Aktiviert/Deaktiviert den Debug-Modus");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer config <pfad> <wert> " + ChatColor.WHITE + "- Ändert einen Konfigurationswert");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer sync " + ChatColor.WHITE + "- Erzwingt eine Datensynchronisierung");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer reload " + ChatColor.WHITE + "- Lädt das Plugin neu");
        sender.sendMessage(ChatColor.YELLOW + "/veinminer help " + ChatColor.WHITE + "- Zeigt diese Hilfe an");
    }
}