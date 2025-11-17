import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader consoleReader;
    private boolean authenticated = false;
    private String user;

    private int reconnectAttempts = 0;
    private final int MAX_RECONNECT_ATTEMPTS = 10;

    public void connect() {
        while (reconnectAttempts <= MAX_RECONNECT_ATTEMPTS && !Thread.currentThread().isInterrupted()) {
            try {
                disconnect();
                socket = new Socket("192.168.0.105", 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                consoleReader = new BufferedReader(new InputStreamReader(System.in));


                ClientLogger.initialize("3");
                FileInit.createPath();
                reconnectAttempts = 0;
                System.out.println("Подключение к серверу установлено!");
                ClientLogger.info("Клиент подключился к серверу");
                showAuthMenu();
                break;
            } catch (Exception e) {
                handleConnectionError(e);
            }
        }
    }

    private void handleConnectionError(Exception e) {
        reconnectAttempts++;
        ClientLogger.warning("Ошибка подключения: " + e.getMessage() + " (попытка " + reconnectAttempts + ")");
        if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
            System.out.println("Превышено максимальное количество попыток подключения. Завершение работы.");
            ClientLogger.severe("Превышено максимальное количество попыток подключения");
            return;
        }

        System.out.println("Ошибка подключения: " + e.getMessage());
        System.out.println("Попытка переподключения " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS);
        System.out.println("Ожидание 5 секунд...");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            ClientLogger.warning("Поток был прерван во время ожидания переподключения");
        }
    }

    private boolean isConnectionError(IOException e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("Connection reset") ||
                        message.contains("Connection refused") ||
                        message.contains("closed") ||
                        message.contains("broken pipe") ||
                        message.contains("reset by peer") ||
                        message.contains("Software caused connection abort")
        );
    }


    private String readFromServer() throws IOException {
        try {
            return in.readLine();
        } catch (IOException e) {
            if (isConnectionError(e)) {
                System.out.println("Потеряно соединение с сервером");
                handleConnectionLoss();
            }
            throw e;
        }
    }

    private void writeToServer(String message) {
        try {
            out.println(message);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Socket closed")) {
                System.out.println("Потеряно соединение с сервером");
                handleConnectionLoss();
            }
        }
    }


    private void handleConnectionLoss() {
        authenticated = false;
        disconnect();
        System.out.println("Пытаемся восстановить соединение...");
        connect();
    }

    private void showAuthMenu() {
        while (!authenticated) {
            try {
                UI.showAuthMenu();
                String choice = consoleReader.readLine();

                switch (choice) {
                    case "1":
                        login();
                        break;
                    case "2":
                        register();
                        break;
                    case "0":
                        disconnect();
                        return;
                    default:
                        System.out.println("Неверный выбор");
                }
            } catch (IOException e) {
                System.out.println("Ошибка ввода: " + e.getMessage());
                if (isConnectionError(e)) {
                    handleConnectionLoss();
                    return;
                }
            }
        }
    }

    private void login() {
        try {
            System.out.print("Логин: ");
            String username = consoleReader.readLine();
            System.out.print("Пароль: ");
            String password = consoleReader.readLine();
            user = username;
            writeToServer("LOGIN:" + username + ":" + password);
            ClientLogger.info("Попытка входа пользователя: " + username);
            String response = readFromServer();
            handleServerResponse(response);
        } catch (IOException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
            ClientLogger.warning("Ошибка ввода при входе: " + e.getMessage());
            if (isConnectionError(e)) {
                handleConnectionLoss();
            }
        }
    }

    private void register() {
        try {
            System.out.print("Логин: ");
            String username = consoleReader.readLine();
            if (!isGoodSign(username)){
                ClientLogger.warning("Неверный формат логина: " + username);
                System.out.println("Имя должно быть из букв");
                return;
            }

            System.out.print("Пароль: ");
            String password = consoleReader.readLine();
            if (!isGoodSign(username)){
                ClientLogger.warning("Неверный формат пароля для пользователя: " + username);
                System.out.println("Пароль должен быть из букв");
                return;
            }

            user = username;
            writeToServer("REGISTER:" + username + ":" + password);
            ClientLogger.info("Попытка регистрации пользователя: " + username);
            String response = readFromServer();
            handleServerResponse(response);
        } catch (IOException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
            ClientLogger.warning("Ошибка ввода при регистрации: " + e.getMessage());
            if (isConnectionError(e)) {
                handleConnectionLoss();
            }
        }
    }

    private boolean isGoodSign(String str){
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isLetter(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void handleServerResponse(String response) {
        if (response == null) {
            System.out.println("\nСервер разорвал соединение\n");
            ClientLogger.warning("Сервер разорвал соединение");
            return;
        }


        if (response.startsWith("AUTH_SUCCESS")) {
            authenticated = true;
            ClientLogger.info("Пользователь " + user + " успешно авторизовался");
            System.out.println("\n\nАвторизация успешна!\n");
            handleMainMenu();
        } else if (response.startsWith("AUTH_FAILED")) {
            ClientLogger.warning("Ошибка авторизации для пользователя: " + user);
            System.out.println("\n\nОшибка авторизации\n");
        } else if (response.startsWith("REGISTER_SUCCESS")) {
            authenticated = true;
            ClientLogger.info("Пользователь " + user + " успешно зарегистрирован");
            System.out.println("\n\nРегистрация успешна!");
            handleMainMenu();
        } else if (response.startsWith("REGISTER_FAILED")) {
            ClientLogger.warning("Ошибка регистрации для пользователя: " + user);
            System.out.println("\n\nОшибка регистрации\n");
        } else if (response.startsWith("USERS_LIST:")) {
            ClientLogger.info("Пользователь " + user + " запросил список пользователей");
            showUsersList(response);
        } else if (response.startsWith("LIST_FILES_RECEIVED:") || response.startsWith("EMPTY")){
            ClientLogger.info("Пользователь " + user + " запросил список файлов");
            handleGetFiles(response);
        } else if (response.startsWith("SEND_TO_RECEIVED")) {
            ClientLogger.info("Пользователь " + user + " успешно отправил файл");
            System.out.println("\n\nФайл успешно отправлен\n");
            handleMainMenu();
        }
        else if (response.startsWith("BAD_SEND_TO")) {
            ClientLogger.warning("Ошибка отправки файла от пользователя: " + user);
            System.out.println("\n\nНе удалось отправить файл\n");
            handleMainMenu();
        }
    }

    private void handleMainMenu() {
        while (authenticated) {
            try {
                UI.showMainMenu();
                String choice = consoleReader.readLine();
                String response;
                switch (choice) {
                    case "1":
                        out.println("LIST_USERS");
                        response = readFromServer();
                        handleServerResponse(response);
                        break;
                    case "2":
                        handleSendFile();
                        break;
                    case "3":
                        out.println("LIST_FILES");
                        response = readFromServer();
                        handleServerResponse(response);
                        break;
                    case "4":
                        UI.showMenuLogging();
                        String logChoice = consoleReader.readLine();
                        ClientLogger.initialize(logChoice);
                        handleMainMenu();
                    case "0":
                        writeToServer("LOGOUT");
                        authenticated = false;
                        System.out.println("Выход из системы");
                        ClientLogger.info("Пользователь " + user + " вышел из системы");
                        return;
                    default:
                        System.out.println("Неверное действие");
                        ClientLogger.warning("Пользователь " + user + " ввел неверную команду: " + choice);
                }
            } catch (IOException e) {
                System.out.println("Ошибка ввода: " + e.getMessage());
                ClientLogger.warning("Ошибка ввода в главном меню: " + e.getMessage());
                if (isConnectionError(e)) {
                    handleConnectionLoss();
                    return;
                }
                return;
            }
        }
    }


    private void handleSendFile() {
        try {
            System.out.print("Введите имя получателя: ");
            String recipient = consoleReader.readLine();
            if (recipient.equals(user)) {
                System.out.println("Вы не можете отправить файл самому себе");
                return;
            }
            if (recipient.isEmpty()){
                System.out.println("Поле не должно быть пустым\n");
                return;
            }

            String filesDir = "files";
            File dir = new File(filesDir);
            if (!dir.exists()) {
                Files.createDirectory(dir.toPath());
            }


            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                System.out.println("Файлы в папке " + filesDir + ":");
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile()) {
                        System.out.println((i + 1) + ". " + files[i].getName() + " " + Files.size(Path.of(files[i].getPath())) + " bytes");
                    }
                }
            } else {
                System.out.println("Папка пуста. Добавьте файлы в папку 'files' рядом с JAR");
                return;
            }

            System.out.print("Выберите номер файла: ");
            String input = consoleReader.readLine();

            String filename;
            try {
                int fileIndex = Integer.parseInt(input) - 1;
                if (fileIndex >= 0 && fileIndex < files.length){
                    filename = files[fileIndex].getPath();
                    String send = Files.readString(Path.of(filename));
                    String shortFilename = Paths.get(filename).getFileName().toString();

                    writeToServer("SEND_TO:" + recipient + ":" + shortFilename + ":" + send + ":" + user);

                    String response = readFromServer();
                    handleServerResponse(response);
                }
                System.out.println("Введите правильное число");
            } catch (NumberFormatException e) {
                System.out.println("Вы должны ввести число");
            }
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
            if (isConnectionError(e)) {
                handleConnectionLoss();
            }
        }
    }

    private void handleGetFiles(String res){
        if (res.equals("EMPTY")) {
            ClientLogger.info("Файлов нет для " + user);
            System.out.println("Файлов нет");
            return;
        }
        System.out.println(res);
        String[] files = res.substring(20).split(":");
        Path directory = Path.of("data" + File.separator  + "received_files" + File.separator);
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                System.out.println("Не удалось создать директорию: " + e.getMessage());
                ClientLogger.warning("Не удалось создать директорию: " + e.getMessage());
                return;
            }
        }


        System.out.println("\nОтправитель     | Название");
        for (String file : files){
            String[] parts = file.split("-");
            System.out.println(parts[0] + "             " + parts[1]);
            Path path = directory.resolve("FROM_" + parts[0] + "_"+parts[1]);
            try {
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
                String data = parts[2].substring(1,parts[2].length() - 1);
                byte[] bytes = data.getBytes();
                Files.write(path, bytes);
                writeToServer("DELETE");
            } catch (IOException e) {
                ClientLogger.warning("Не удалось создать/записать файл: " + e.getMessage());
                System.out.println("Не удалось создать/записать файл: " + e.getMessage());
            }
        }
        System.out.println();
    }

    private void showUsersList(String message) {
        if (message.equals("USERS_LIST:")) {
            System.out.println("Список пользователей пуст");
            return;
        }
        String usersData = message.replace("USERS_LIST:", "");
        System.out.println("\n=== СПИСОК ПОЛЬЗОВАТЕЛЕЙ ===");

        String[] users = usersData.split("\\|");
        for (String user : users) {
            System.out.println(user);
        }
        System.out.println("============================\n");
    }

    private void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }
}