public class UI {

    public static void showAuthMenu() {
        System.out.println("=== ФАЙЛООБМЕННИК ===");
        System.out.println("1. Войти");
        System.out.println("2. Зарегистрироваться");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    public static void showMainMenu(){
        System.out.println("1. Показать всеx пользователей");
        System.out.println("2. Отправить файл пользователю");
        System.out.println("3. Получить отправленные мне файлы");
        System.out.println("4. Настройки логирования");
        System.out.println("0. Выйти");
        System.out.print("Выберите действие: ");
    }

    public static void showMenuLogging(){
        System.out.println("=== Настройка логирования ===");
        System.out.println("1. Без логирования");
        System.out.println("2. Только WARNING (критические ошибки)");
        System.out.println("3. WARNING + INFO (все события)");
        System.out.print("Выберите уровень (1-3): ");
    }
}
