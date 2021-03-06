package controllers;

import entities.Customer;
import entities.Order;
import entities.Paczkamat;
import entities.Stash;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import netscape.javascript.JSObject;
import services.DataSource;
import services.SessionStore;
import web.WebViewConnector;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;

/**
 * Klasa CustomerController odpowiedzialna za obsługę wszystkich interakcji pomiędzy użytkownikiem
 * zalogowanym jako zwykły użtkownik, a oddzielnym GUI przygotowanym dla tego użykownika.
 */
public class CustomerController {


    //Szereg pól odpowiadających FXML-owym obiektom zastosowanym w GUI
    @FXML
    private TableView<Order> ordersListSentTo;

    @FXML
    private Text loggedInAs;

    @FXML
    private Text SendTextHint;

    @FXML
    private TabPane tabPane;

    @FXML
    private WebView orderWebView;

    @FXML
    private ComboBox<Customer> recipientComboBox;

    @FXML
    private ComboBox<String> dimensionComboBox;

    @FXML
    private Text sendPaczkamatName;

    @FXML
    private ComboBox<Stash> sendStash;

    @FXML
    private Text receivePaczkamatName;

    @FXML
    private ComboBox<Stash> receiveStash;

    @FXML
    private TableView<Order> ordersListReceivingFrom;

    //Koniec Szeregu pól odpowiadającym FXML-owym obiektom zastosowanym w GUI

    private final ObservableList<String> dimensions = FXCollections.observableArrayList("SMALL", "MEDIUM", "LARGE");
    private       WebViewConnector webViewConnector;
    private       Paczkamat sendPaczkamat;
    private       Paczkamat receivePaczkamat;

    /**
     * @param event
     * Metoda wykonująca się w momencie gdy użytkownik naciska na przycisk LogOut. Odpowiada
     * za wylogowanie z sessji obecnego użytkownika oraz wyświetlenie nowego layoutu ukazującego
     * ekran logowania.
     */
    @FXML
    void onLogoutClicked(ActionEvent event) {
        DataSource.setLoggedUser(null);
        SessionStore.setLoggedIn(false);
        System.out.println(SessionStore.isLoggedIn());
        showNewlayout("layout/login_screen.fxml", event);
    }

    /**
     * Metoda wywoływana po naciśnięciu przez użytkownika przcisku "Order", jest odpowiedzialna za zweryfikowanie
     * danych wprowadzonych przez użytkownika i ewentualne wyprowadzenie go z błędu w przypadku jego pomyłki.
     * Po pomyślnej weryfikacji, zamówienie dodawane jest do bazy, o czym informowany jest użytkownik.
     */
    @FXML
    void onOrderClicked() {
        Customer customer       = SessionStore.getUser();
        Customer recipient      = recipientComboBox.getValue();
        String   dimension      = dimensionComboBox.getValue();
        Stash    senderStash    = sendStash.getValue();
        Stash    receiverStash  = receiveStash.getValue();

        if (recipient == null) {
            SendTextHint.setText("Choose recipient to send order");
            return;
        }

        if (dimension == null) {
            SendTextHint.setText("Choose package dimension to send order");
            return;
        }

        if (sendPaczkamat == null) {
            SendTextHint.setText("Choose send paczkamat from map to send order");
            return;
        }

        if (receivePaczkamat == null) {
            SendTextHint.setText("Choose recipient paczkamat from map to send order");
            return;
        }

        if (senderStash == null) {
            SendTextHint.setText("Choose sender stash to send order");
            return;
        }

        if (receiverStash == null) {
            SendTextHint.setText("Choose receiver stash to send order");
            return;
        }

        Order order = new Order();
        order.setSender(customer);
        order.setOrderStatus("AWAITING_PICKUP");
        order.setReceiver(recipient);
        Timestamp sendTime = Timestamp.from(Instant.now());
        order.setSendDatetime(sendTime);
        ZonedDateTime zonedDateTime = sendTime.toInstant().atZone(ZoneId.of("UTC"));
        Timestamp receiveTime = Timestamp.from(zonedDateTime.plus(3, ChronoUnit.DAYS).toInstant());
        order.setReceiveDatetime(receiveTime);
        BigDecimal price = BigDecimal.ONE;

        switch (dimension) {
            case "SMALL" -> price = BigDecimal.valueOf(9.90);
            case "MEDIUM" -> price = BigDecimal.valueOf(16.90);
            case "LARGE" -> price = BigDecimal.valueOf(28.90);
            default -> SendTextHint.setText("Wrong dimension");
        }

        order.setPrice(price);
        order.setSenderStash(senderStash);
        order.setReceiverStash(receiverStash);

        DataSource.addOrder(order);

        senderStash.getOrdersToSend().add(order);
        receiverStash.getOrdersToReceive().add(order);
        customer.getOrdersAsSender().add(order);
        recipient.getOrdersAsReceiver().add(order);

        SendTextHint.setText("Order sent to: " + recipient.getName()+" "+recipient.getLastName());
        updateStashesList(dimensionComboBox.getValue());
    }

    /**
     * Metoda wywoływana na początku "istnienia" Controllera, jej zadaniami są:
     *  - przygotowanie WebView poprzez utworzenie nowego obiektu WebViewConnector, któremu następnie przekazywane są
     *    zmienna odpowiadająca FXML-owemu obiektowi klasy WebView oraz plik .html, który zawiera kod, który wykona się w WebView
     *  - dodanie listenera do obiektu klasy TabPane reagującego na wybranie innej zakładki przez użytkownika
     *  - zaimplementowanie możliwości sortowania zamówień przypisanych użytkownikowi przy pomocy daty
     *  - zaimplementowanie funkcjonalności pozwalającej na reagowanie na wszystkie zmiany wprowadzane przez użytkownika
     *    związane z parametrami opisującymi składane przez niego zamówienie.
     */
    @FXML
    void initialize() {
        webViewConnector = new WebViewConnector();
        setupWebView(orderWebView);
        loggedInAs.setText("Logged in as: " + SessionStore.getUser().getName() + " " + SessionStore.getUser().getLastName());
        dimensionComboBox.setItems(dimensions);
        dimensionComboBox.valueProperty().addListener((observableValue, s, newValue) -> {
            if (newValue != null) {
                updateStashesList(newValue);
            }
        });


        recipientComboBox.setCellFactory(new Callback<>() {

            @Override
            public ListCell<Customer> call(ListView<Customer> customerListView) {

                return new ListCell<>() {
                    @Override
                    protected void updateItem(Customer customer, boolean b) {
                        super.updateItem(customer, b);

                        if (customer != null) {
                            setText(customer.getName() + " " + customer.getLastName());
                        } else {
                            setText(null);
                        }
                    }
                };
            }
        });

        recipientComboBox.setItems(DataSource.getCustomers());

        webViewConnector.receivePaczkamatProperty().addListener((observableValue, oldPaczkamat, newPaczkamat) -> {
            receivePaczkamat = newPaczkamat;
            receivePaczkamatName.setText(newPaczkamat.getName());
            SendTextHint.setText("Fill in the details and select \"Order\" to send the package or select another paczkamat if You wish.");
            updateStashesList(dimensionComboBox.getValue());

            receiveStash.setCellFactory(new Callback<>() {

                @Override
                public ListCell<Stash> call(ListView<Stash> stashListView) {

                    return new ListCell<>() {
                        @Override
                        protected void updateItem(Stash stash, boolean b) {
                            super.updateItem(stash, b);

                            if (stash != null) {
                                setText(stash.toString());
                            } else {
                                setText(null);
                            }
                        }
                    };
                }

            });

        });

        webViewConnector.sendPaczkamatProperty().addListener((observableValue, oldPaczkamat, newPaczkamat) -> {
            sendPaczkamat = newPaczkamat;
            sendPaczkamatName.setText(newPaczkamat.getName());
            SendTextHint.setText("Select the receiver's paczkamat");

            updateStashesList(dimensionComboBox.getValue());

            sendStash.setCellFactory(new Callback<>() {

                @Override
                public ListCell<Stash> call(ListView<Stash> stashListView) {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(Stash stash, boolean b) {
                            super.updateItem(stash, b);

                            if (stash != null) {
                                setText(stash.toString());
                            } else {
                                setText(null);
                            }
                        }
                    };
                }
            });
        });

        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (ov, t, t1) -> {
                    System.out.println("Tab Selection changed to " + t1.getText());

                    if (t1.getId().equals("trackTab")) {
                        System.out.println(SessionStore.getUser().getName());
                        Collection<Order> ordersAsSender   = SessionStore.getUser().getOrdersAsSender();
                        Collection<Order> ordersAsReceiver = SessionStore.getUser().getOrdersAsReceiver();

                        ObservableList<Order> ordersSent = FXCollections.observableArrayList(ordersAsSender);
                        ObservableList<Order> ordersReceived = FXCollections.observableArrayList(ordersAsReceiver);

                        ordersListSentTo.setItems(ordersSent);
                        ordersListReceivingFrom.setItems(ordersReceived);
                        System.out.println("Track tab");

                    }
                }
        );
    }

    /**
     * @param newValue
     * Metoda, która update'uje listę skrytek dostępnych w danym paczkamacie:
     * W sytuacji, w której użytkownik dodał zamówienie, dana skrytka zostaje usuwana z listy dostępnych skrytek
     */
    private void updateStashesList(String newValue) {
        addPaczkamats(newValue, sendPaczkamat, sendStash);
        addPaczkamats(newValue, receivePaczkamat, receiveStash);
    }


    /**
     * @param newValue
     * @param receivePaczkamat
     * @param receiveStash
     *
     * Metoda która dodaje nowy paczkamat do listy dostępnych paczkamatów, jest ona wywoływana w momencie kiedy
     * użytkownik wybierze z mapy paczkamat, którego do tej pory nie było w bazie
     */
    private void addPaczkamats(String newValue, Paczkamat receivePaczkamat, ComboBox<Stash> receiveStash) {
        if (receivePaczkamat != null) {
            receiveStash.setItems(FXCollections.observableArrayList(receivePaczkamat.getStashes()).filtered(stash -> {
                if (stash.getDimension().equals(newValue)){
                    for (Order order: stash.getOrdersToSend()) {
                        if (order.getOrderStatus().equals("AWAITING_PICKUP")){
                            return false;
                        }
                    }

                    for (Order order: stash.getOrdersToReceive()) {
                        if (order.getOrderStatus().equals("IN_SHIPMENT") ||
                                order.getOrderStatus().equals("IN_DELIVERY") ||
                                order.getOrderStatus().equals("AWAITING_PICKUP")
                        ){
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }));
        }
    }

    /**
     * @param webView FXML - owy obiekt klasy WebView, w którym chcemy uruchomić kod HTML
     *
     * Metoda ta wczytuje plik HTML do obiektu klasy WebView dodatkowo implementując Listener, który pozwala na
     * zareagowanie w momencie gdy użytkownik wybierze paczkamat.
     */
    private void setupWebView(WebView webView) {
        WebEngine webEngine = webView.getEngine();
        webEngine.getLoadWorker().stateProperty()
                .addListener((observable, oldValue, newValue) -> {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("app", webViewConnector);
                });
        webEngine.load(getClass().getResource("/webview/" + "customer_map.html").toString());
    }

    /**
     * @param path Ścieżka do pliku .fxml zawierającego layout do nowego okna, które chcemy pokazać
     * @param event obiekt klasy ActionEvent, dzięki któremu mamy możliwość ukrycia layoutu, w którym znajdował się przycisk
     * który wywołał tą metodę.
     */
    private void showNewlayout(String path, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource(path)));
            Stage stage = new Stage();
            stage.setTitle("Login");
            stage.setScene(new Scene(root));
            stage.show();
            // Hide current window
            ((Node)(event.getSource())).getScene().getWindow().hide();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
