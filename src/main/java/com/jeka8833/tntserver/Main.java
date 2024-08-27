package com.jeka8833.tntserver;

import com.jeka8833.tntserver.database.RemoteDB;
import picocli.CommandLine;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@CommandLine.Command(name = "TNTServer")
public final class Main implements Runnable {
    public static final Main INSTANCE = new Main();

    @CommandLine.Option(names = "--grafana_url", description = "Remote logging and metrics. Grafana URL")
    public Optional<String> grafanaUrl = Optional.empty();

    @CommandLine.Option(names = "--grafana_username", description = "Remote logging and metrics. Grafana Username")
    public Optional<String> grafanaUsername = Optional.empty();

    @CommandLine.Option(names = "--grafana_password", description = "Remote logging and metrics. Grafana Password")
    public Optional<String> grafanaPassword = Optional.empty();

    @CommandLine.Option(names = "--grafana_metrics_interval", description = "Remote logging and metrics. " +
            "Metrics Send Interval. (default: ${DEFAULT-VALUE})")
    public Optional<Duration> grafanaMetricsInterval = Optional.ofNullable(Duration.ofSeconds(10));

    @CommandLine.Option(names = "--swear_dictionary_path", description = "The location of the folder where " +
            "dictionaries with bad words are stored.")
    public Optional<Path> swearDictionary = Optional.empty();

    @CommandLine.Option(names = "--analytics_path", description = "Location where player analytics will be stored.")
    public Optional<Path> analycitcsPath = Optional.empty();

    @CommandLine.Option(names = "--analytics_max_folder_size", description = "The maximum size of the folder " +
            "with player analytics in bytes. (default: ${DEFAULT-VALUE})")
    public long analyticsMaxFolderSize = 5L * 1024L * 1024L * 1024L;

    @CommandLine.Option(names = "--hypixel_api_key", description = "Hypixel API Key.")
    public Optional<UUID> hypixelApiKey = Optional.empty();

    @CommandLine.Option(names = "--database_url", description = "PostgreSQL Database URL. (default: ${DEFAULT-VALUE})",
            required = true)
    public String databaseURL = "localhost:5432/postgres";

    @CommandLine.Option(names = "--database_user", description = "PostgreSQL Database Password. " +
            "(default: ${DEFAULT-VALUE})", required = true)
    public String databaseUser = "postgres";

    @CommandLine.Option(names = "--database_password", description = "PostgreSQL Database Password. " +
            "(default: ${DEFAULT-VALUE})", required = true)
    public String databasePassword;

    @CommandLine.Option(names = "--server_port", description = "Server port. (default: ${DEFAULT-VALUE})")
    public int serverPort = 8833;

    public static void main(String[] args) {
        int error = new CommandLine(INSTANCE).execute(args);

        System.exit(error);
    }

    @Override
    public void run() {
        try {
            TNTServer.loadServer();

            try {
                Thread.currentThread().join();  // Wait for the server to stop
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } finally {
            RemoteDB.saveAndClose();
        }
    }
}
