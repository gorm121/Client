import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader consoleReader;
    private boolean authenticated = false;

    public void connect() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            consoleReader = new BufferedReader(new InputStreamReader(System.in));

            // УБРАЛИ startListener() - всё в одном потоке
            showAuthMenu();

        } catch (Exception e) {
            System.out.println("Ошибка подключения: " + e.getMessage());
        }
    }

    private void showAuthMenu() {
        while (!authenticated) {
            try {
                UI.showAuthMenu();
                System.out.print("Выберите действие: ");
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

            out.println("LOGIN:" + username + ":" + password);
            String response = in.readLine(); // ЖДЕМ ОТВЕТ СЕРВЕРА
            handleServerResponse(response);
        } catch (IOException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
        }
    }

    private void register() {
        try {
            System.out.print("Логин: ");
            String username = consoleReader.readLine();
            System.out.print("Пароль: ");
            String password = consoleReader.readLine();

            out.println("REGISTER:" + username + ":" + password);
            String response = in.readLine(); // ЖДЕМ ОТВЕТ СЕРВЕРА
            handleServerResponse(response);
        } catch (IOException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
        }
    }

    private void handleServerResponse(String response) {
        if (response == null) {
            System.out.println("Сервер разорвал соединение");
            return;
        }

        System.out.println("Сервер: " + response);

        if (response.startsWith("AUTH_SUCCESS")) {
            authenticated = true;
            System.out.println(" Авторизация успешна!");
            handleMainMenu();
        } else if (response.startsWith("AUTH_FAILED")) {
            System.out.println(" Ошибка авторизации");
        } else if (response.startsWith("REGISTER_SUCCESS")) {
            authenticated = true;
            System.out.println(" Регистрация успешна!");
            handleMainMenu();
        } else if (response.startsWith("REGISTER_FAILED")) {
            System.out.println(" Ошибка регистрации");
        } else if (response.startsWith("USERS_LIST:")) {
            showUsersList(response);
        }
    }

    private void handleMainMenu() {
        while (authenticated) {
            try {
                UI.showMainMenu();
                String choice = consoleReader.readLine();

                switch (choice) {
                    case "1":
                        out.println("LIST_USERS");
                        String response = in.readLine(); // ЖДЕМ ОТВЕТ
                        handleServerResponse(response);
                        break;
                    case "2":
                        handleSendFile();
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
            System.out.print("Введите имя файла: ");
            String filename = consoleReader.readLine();

            out.println("SEND_TO:" + recipient + ":" + filename);
            String response = in.readLine(); // ЖДЕМ ОТВЕТ
            handleServerResponse(response);
        } catch (IOException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
        }
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