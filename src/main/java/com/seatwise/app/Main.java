package com.seatwise.app;

import java.nio.file.Path;

/**
 * Entry point.
 * Interactive: {@code java -jar target/seatwise.jar}
 * Demo: {@code java -jar target/seatwise.jar --demo}
 * Optional: {@code --db path/to/file.db}
 */
public final class Main {

    private static final Path DEFAULT_DB = Path.of("data", "seatwise.db");
    private static final Path DEFAULT_DEMO_DB = Path.of("data", "seatwise-demo.db");

    private Main() {
    }

    public static void main(String[] args) {
        LaunchOptions options = LaunchOptions.parse(args);
        if (options.demo()) {
            DemoRunner.run(options.databaseFile());
        } else {
            try (SeatWiseApp app = SeatWiseApp.open(AppConfig.interactive(options.databaseFile()))) {
                new ConsoleUI(app, System.in, System.out).run();
            }
        }
    }

    record LaunchOptions(boolean demo, Path databaseFile) {

        static LaunchOptions parse(String[] args) {
            boolean demo = false;
            Path database = null;
            for (int i = 0; i < args.length; i++) {
                if ("--demo".equals(args[i])) {
                    demo = true;
                } else if ("--db".equals(args[i]) && i + 1 < args.length) {
                    i++;
                    database = Path.of(args[i]);
                }
            }
            if (database == null) {
                database = demo ? DEFAULT_DEMO_DB : DEFAULT_DB;
            }
            return new LaunchOptions(demo, database);
        }
    }
}
