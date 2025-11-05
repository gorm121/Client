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

    public void connect() {
        try {
            socket = new Socket("192.168.0.105", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            consoleReader = new BufferedReader(new InputStreamReader(System.in));

            FileInit.createPath();
            showAuthMenu();

        } catch (Exception e) {
            System.out.println("Ошибка подключения: " + e.getMessage());
        }
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
                return;
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
            out.println("LOGIN:" + username + ":" + password);
            String response = in.readLine();
            handleServerResponse(response);
        } catch (IOException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
        }
    }

    private void register() {
        try {
            System.out.print("Логин: ");
            String username = consoleReader.readLine();
            if (!isGoodSign(username)){
                System.out.println("Имя должно быть из букв");
                return;
            }

            System.out.print("Пароль: ");
            String password = consoleReader.readLine();
            if (!isGoodSign(username)){
                System.out.println("Пароль должен быть из букв");
                return;
            }

            user = username;
            out.println("REGISTER:" + username + ":" + password);
            String response = in.readLine();
            handleServerResponse(response);
        } catch (IOException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
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
            return;
        }

//        System.out.println("Сервер: " + response);

        if (response.startsWith("AUTH_SUCCESS")) {
            authenticated = true;
            System.out.println("\n\nАвторизация успешна!\n");
            handleMainMenu();
        } else if (response.startsWith("AUTH_FAILED")) {
            System.out.println("\n\nОшибка авторизации\n");
        } else if (response.startsWith("REGISTER_SUCCESS")) {
            authenticated = true;
            System.out.println("\n\nРегистрация успешна!");
            handleMainMenu();
        } else if (response.startsWith("REGISTER_FAILED")) {
            System.out.println("\n\nОшибка регистрации\n");
        } else if (response.startsWith("USERS_LIST:")) {
            showUsersList(response);
        } else if (response.startsWith("LIST_FILES_RECEIVED:") || response.startsWith("EMPTY")){
            handleGetFiles(response);
        } else if (response.startsWith("SEND_TO_RECEIVED")) {
            System.out.println("\n\nФайл успешно отправлен\n");
            handleMainMenu();
        }
        else if (response.startsWith("BAD_SEND_TO")) {
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
                        response = in.readLine();
                        handleServerResponse(response);
                        break;
                    case "2":
                        handleSendFile();
                        break;
                    case "3":
                        out.println("LIST_FILES");
                        response = in.readLine();
                        handleServerResponse(response);
                        break;
                    case "0":
                        out.println("LOGOUT");
                        authenticated = false;
                        System.out.println("Выход из системы");
                        return;
                    default:
                        System.out.println("Неверное действие");
                }
            } catch (IOException e) {
                System.out.println("Ошибка ввода: " + e.getMessage());
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

            System.out.println("Файлы в папке " + filesDir + ":");
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
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

                    out.println("SEND_TO:" + recipient + ":" + shortFilename + ":" + send + ":" + user);

                    String response = in.readLine();
                    handleServerResponse(response);
                }
                System.out.println("Введите правильное число");
            } catch (NumberFormatException e) {
                System.out.println("Вы должны ввести число");
            }
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
        }
    }

    private void handleGetFiles(String res){
        if (res.equals("EMPTY")) {
            System.out.println("Файлов нет");
            return;
        }
        String[] files = res.substring(20).split(":");
        Path directory = Path.of("data" + File.separator  + "received_files" + File.separator);
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                System.out.println("Не удалось создать директорию: " + e.getMessage());
                return;
            }
        }

        System.out.println();
        System.out.println("Отправитель     | Название");
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
                out.println("DELETE");
            } catch (IOException e) {
                System.out.println("Не удалось создать/записать файл: " + e.getMessage());
            }
        }
        System.out.println();

    }

    private void showUsersList(String message) {
        System.out.println("\n=== СПИСОК ПОЛЬЗОВАТЕЛЕЙ ===");
        String usersData = message.replace("USERS_LIST:", "");
        String[] users = usersData.split("\\|");
        for (String user : users) {
            System.out.println(user);
        }
        System.out.println("============================\n");
    }

    private void disconnect() {
        try {
            if (socket != null) socket.close();
            if (consoleReader != null) consoleReader.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}