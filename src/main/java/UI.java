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
        System.out.println("0. Выйти");
        System.out.print("Выберите действие: ");
    }
}
