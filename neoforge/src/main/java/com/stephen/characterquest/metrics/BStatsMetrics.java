package com.stephen.characterquest.metrics;

import net.minecraft.SharedConstants;
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
import java.util.function.Supplier;

public final class BStatsMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger(BStatsMetrics.class);

    private BStatsMetrics() {}

    public static void init(String modVersion, int pluginId, Path configDir,
                            Supplier<Integer> playerAmountSupplier,
                            Supplier<Integer> onlineModeSupplier,
                            Supplier<String> serverNameSupplier) {
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
                json -> appendPlatformData(json, playerAmountSupplier, onlineModeSupplier, serverNameSupplier),
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

    private static void appendPlatformData(JsonObjectBuilder json,
                                           Supplier<Integer> playerAmountSupplier,
                                           Supplier<Integer> onlineModeSupplier,
                                           Supplier<String> serverNameSupplier) {
        json.appendField("playerAmount", playerAmountSupplier.get());
        json.appendField("onlineMode", onlineModeSupplier.get());
        json.appendField("serverVersion", SharedConstants.getCurrentVersion().getName());
        json.appendField("serverName", serverNameSupplier.get());
        json.appendField("javaVersion", System.getProperty("java.version"));
        json.appendField("osName", System.getProperty("os.name"));
        json.appendField("osArch", System.getProperty("os.arch"));
        json.appendField("osVersion", System.getProperty("os.version"));
        json.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }
}
