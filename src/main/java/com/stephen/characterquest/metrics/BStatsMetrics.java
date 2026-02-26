package com.stephen.characterquest.metrics;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bstats.MetricsBase;
import org.bstats.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

public final class BStatsMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger(BStatsMetrics.class);

    private BStatsMetrics() {}

    public static void init(String modVersion, int pluginId) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path bstatsDir = configDir.resolve("bStats");
        Path configFile = bstatsDir.resolve("config.properties");

        Properties properties = new Properties();
        properties.setProperty("enabled", "true");
        properties.setProperty("serverUuid", UUID.randomUUID().toString());
        properties.setProperty("logFailedRequests", "false");
        properties.setProperty("logSentData", "false");
        properties.setProperty("logResponseStatusText", "false");

        try {
            Files.createDirectories(bstatsDir);
            if (Files.exists(configFile)) {
                try (InputStream in = Files.newInputStream(configFile)) {
                    properties.load(in);
                }
            } else {
                try (OutputStream out = Files.newOutputStream(configFile)) {
                    properties.store(out, "bStats config");
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed to load bStats config", ex);
        }

        boolean enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
        String serverUuid = properties.getProperty("serverUuid", UUID.randomUUID().toString());
        boolean logFailedRequests = Boolean.parseBoolean(properties.getProperty("logFailedRequests", "false"));
        boolean logSentData = Boolean.parseBoolean(properties.getProperty("logSentData", "false"));
        boolean logResponseStatusText = Boolean.parseBoolean(properties.getProperty("logResponseStatusText", "false"));

        new MetricsBase(
                "server-implementation",
                serverUuid,
                pluginId,
                enabled,
                BStatsMetrics::appendPlatformData,
                json -> json.appendField("pluginVersion", modVersion),
                Runnable::run,
                () -> true,
                (message, throwable) -> {
                    if (logFailedRequests) {
                        LOGGER.warn(message, throwable);
                    }
                },
                message -> {
                    if (logSentData || logResponseStatusText) {
                        LOGGER.info(message);
                    }
                },
                logFailedRequests,
                logSentData,
                logResponseStatusText,
                false
        );
    }

    private static void appendPlatformData(JsonObjectBuilder json) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        json.appendField("playerAmount", server == null ? 0 : server.getPlayerCount());
        json.appendField("onlineMode", server != null && server.usesAuthentication() ? 1 : 0);
        json.appendField("serverVersion", SharedConstants.getCurrentVersion().getName());
        json.appendField("serverName", "Forge");
        json.appendField("javaVersion", System.getProperty("java.version"));
        json.appendField("osName", System.getProperty("os.name"));
        json.appendField("osArch", System.getProperty("os.arch"));
        json.appendField("osVersion", System.getProperty("os.version"));
        json.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }
}
