import java.io.IOException;
import java.util.logging.*;

public class ClientLogger {
    private static Logger logger;
    private static boolean isInitialized = false;


    public static void initialize(String choice) {
        if (isInitialized) return;

        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                rootLogger.removeHandler(handler);
            }
        }

        switch (choice) {
            case "1":
                disableLogging();
                System.out.println("Логирование отключено");
                break;
            case "2":
                setupWarningLogging();
                System.out.println("Логирование WARNING включено");
                break;
            case "3":
                setupInfoLogging();
                System.out.println("Логирование INFO + WARNING включено");
                break;
            default:
                System.out.println("Неверный выбор, логирование отключено");
                disableLogging();
        }
        isInitialized = true;
    }

    public static void info(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    public static void warning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    public static void severe(String message) {
        if (logger != null) {
            logger.severe(message);
        }
    }

    private static void disableLogging() {
        logger = Logger.getLogger("ClientLogger");
        logger.setLevel(Level.OFF);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
    }

    private static void setupWarningLogging() {
        try {
            FileHandler fileHandler = new FileHandler("data/logs/client.log", 0, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.WARNING);

            logger = Logger.getLogger("ClientLogger");
            logger.setLevel(Level.WARNING);

            removeConsoleHandlers(logger);
            logger.addHandler(fileHandler);

        } catch (IOException e) {
            System.out.println("Ошибка настройки логирования: " + e.getMessage());
        }
    }

    private static void setupInfoLogging() {
        try {
            FileHandler fileHandler = new FileHandler("data/logs/client.log", 0, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);

            logger = Logger.getLogger("ClientLogger");
            logger.setLevel(Level.INFO);

            removeConsoleHandlers(logger);
            logger.addHandler(fileHandler);

        } catch (IOException e) {
            System.out.println("Ошибка настройки логирования: " + e.getMessage());
        }
    }

    private static void removeConsoleHandlers(Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                logger.removeHandler(handler);
            }
        }
    }
}