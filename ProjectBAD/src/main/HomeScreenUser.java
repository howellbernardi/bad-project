package main;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import util.Connect;

import java.sql.*;

public class HomeScreenUser {
    private Scene homeScene;
    private ListView<Donut> donutListView;
    private Label nameLabel, descriptionLabel, priceLabel;
    private Spinner<Integer> quantitySpinner;
    private Button addToCartButton;

    private String currentUserID;

    public Scene getHomeScene(Stage primaryStage, String userID) {
        this.currentUserID = userID;

        if (homeScene == null) {
            BorderPane mainLayout = new BorderPane();
            VBox root = new VBox(10);
            root.setPadding(new Insets(10));

            MenuBar menuBar = createMenuBar(primaryStage);
            mainLayout.setTop(menuBar);

            String username = fetchUsername(currentUserID);

            Label welcomeLabel = new Label("Hello, " + username);
            root.getChildren().add(welcomeLabel);

            donutListView = new ListView<>();
            donutListView.setPlaceholder(new Label("No donuts available"));
            donutListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> displayDonutDetails(newSelection));
            root.getChildren().add(donutListView);

            VBox detailsBox = new VBox(5);
            nameLabel = new Label("Name: ");
            descriptionLabel = new Label("Description: ");
            priceLabel = new Label("Price: ");
            detailsBox.getChildren().addAll(nameLabel, descriptionLabel, priceLabel);

            quantitySpinner = new Spinner<>(1, 999, 1);
            detailsBox.getChildren().add(new Label("Quantity: "));
            detailsBox.getChildren().add(quantitySpinner);

            addToCartButton = new Button("Add to Cart");
            addToCartButton.setDisable(true);
            addToCartButton.setOnAction(e -> addToCart());
            detailsBox.getChildren().add(addToCartButton);
            root.getChildren().add(detailsBox);

            mainLayout.setCenter(root);
            homeScene = new Scene(mainLayout, 800, 600);

            loadDonuts();
        }
        return homeScene;
    }

    private String fetchUsername(String userId) {
        String username = "User";
        String query = "SELECT UserName FROM msuser WHERE UserID = ?";

        try (Connection conn = Connect.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                username = rs.getString("UserName");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to fetch username: " + e.getMessage());
        }
        return username;
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        Menu menuDashboard = new Menu("Dashboard");

        MenuItem homeItem = new MenuItem("Home");
        homeItem.setDisable(true);
        homeItem.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Navigation", "You are on the Home screen!"));

        MenuItem cartItem = new MenuItem("Cart");
        cartItem.setOnAction(e -> openCartScreen(primaryStage));

        menuDashboard.getItems().addAll(homeItem, cartItem);

        Menu menuLogout = new Menu("Logout");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> primaryStage.setScene(new LoginScene(primaryStage).getScene()));
        menuLogout.getItems().add(logoutItem);
        menuBar.getMenus().addAll(menuDashboard, menuLogout);
        return menuBar;
    }

    private void loadDonuts() {
        String query = "SELECT * FROM msdonut";
        try (Connection conn = Connect.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Donut donut = new Donut(rs.getString("DonutID"), rs.getString("DonutName"), rs.getString("DonutDescription"), rs.getDouble("DonutPrice"));
                donutListView.getItems().add(donut);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load donuts: " + e.getMessage());
        }
    }

    private void displayDonutDetails(Donut donut) {
        if (donut != null) {
            nameLabel.setText("Name: " + donut.getName());
            descriptionLabel.setText("Description: " + donut.getDescription());
            priceLabel.setText("Price: " + donut.getPrice());
            addToCartButton.setDisable(false);
        } else {
            nameLabel.setText("Name: ");
            descriptionLabel.setText("Description: ");
            priceLabel.setText("Price: ");
            addToCartButton.setDisable(true);
        }
    }

    private void addToCart() {
        Donut selectedDonut = donutListView.getSelectionModel().getSelectedItem();
        int quantity = quantitySpinner.getValue();

        if (selectedDonut != null) {
            String query = "INSERT INTO cart (UserID, DonutID, Quantity) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Quantity = Quantity + ?";
            try (Connection conn = Connect.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, currentUserID);
                pstmt.setString(2, selectedDonut.getId());
                pstmt.setInt(3, quantity);
                pstmt.setInt(4, quantity);
                pstmt.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Cart", "Added to Cart: " + selectedDonut.getName() + " x" + quantity);
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add to cart: " + e.getMessage());
            }
        }
    }

    private void openCartScreen(Stage primaryStage) {
        CartScene cs = new CartScene(currentUserID);
        cs.show(primaryStage);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    class Donut {
        private String id, name, description;
        private double price;

        public Donut(String id, String name, String description, double price) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public double getPrice() {
            return price;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
