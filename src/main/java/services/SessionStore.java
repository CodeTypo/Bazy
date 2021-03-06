package services;

import entities.Customer;

/**
 * Klasa reprezentującą pojedynczą sesję podczas korzystania z aplikacji, zawiera metody pozwalające na zalogowanie
 * użytkowika, weryfikację jego uprawnień, wylogowanie użytkownika, czy też sprawdzenie czy w sesji zalogowany jest
 * aktualnie jakiś użytkownik.
 */
public class SessionStore {
    private static boolean admin = false;
    private static boolean loggedIn = false;
    private static Customer user;
    public static Customer getUser() {
        return user;
    }

    public static void setUser(Customer user) {
        if (!loggedIn) {
            loggedIn = true;
            SessionStore.user = user;
        }
    }

    public static boolean isAdmin() {
        return admin;
    }

    public static void setAdmin(boolean admin) {
        SessionStore.admin = admin;
    }

    public static void setLoggedIn(boolean loggedIn) {
        SessionStore.loggedIn = loggedIn;
    }

    public static boolean isLoggedIn() {
        return loggedIn;
    }

    private SessionStore() {}
}
