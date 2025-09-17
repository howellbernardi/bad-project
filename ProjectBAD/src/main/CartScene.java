package main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import util.Connect;

import java.sql.*;

public class CartScene {

    private TableView<CartItem> cartTable;
    private Label totalPriceLabel;
    private ObservableList<CartItem> cartItems;
    private final String currentUserID;

    public CartScene(String userID) {
        this.currentUserID = userID;
    }

    public void show(Stage stage) {
        BorderPane root = new BorderPane();

        MenuBar menuBar = createMenuBar(stage);
        root.setTop(menuBar);

        Label titleLabel = new Label("Your Cart");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        cartTable = new TableView<>();
        setupCartTable();

        totalPriceLabel = new Label("Rp. 0");
        totalPriceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        loadCartData();

        Button checkoutButton = new Button("Checkout Selected Items");
        checkoutButton.setOnAction(e -> checkoutSelectedItems());

        HBox bottomBox = new HBox(10, totalPriceLabel, checkoutButton);
        bottomBox.setPadding(new Insets(10));

        VBox centerBox = new VBox(10, titleLabel, cartTable);
        centerBox.setPadding(new Insets(10));
        root.setCenter(centerBox);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Cart");
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu menuDashboard = new Menu("Dashboard");

        MenuItem homeItem = new MenuItem("Home");
        homeItem.setOnAction(e -> {
            HomeScreenUser home = new HomeScreenUser();
            stage.setScene(home.getHomeScene(stage, currentUserID)); 
        });

        MenuItem cartItem = new MenuItem("Cart");
        cartItem.setDisable(true);
        cartItem.setOnAction(e -> {
            CartScene cartScene = new CartScene(currentUserID);
            cartScene.show(stage);
        });

        menuDashboard.getItems().addAll(homeItem, cartItem);

        Menu menuLogout = new Menu("Logout");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            showAlert(AlertType.INFORMATION, "Logout", "You have been logged out.");
            stage.setScene(new LoginScene(stage).getScene());
        });
        menuLogout.getItems().add(logoutItem);

        menuBar.getMenus().addAll(menuDashboard, menuLogout);

        return menuBar;
    }

    private void setupCartTable() {
        TableColumn<CartItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<CartItem, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<CartItem, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<CartItem, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        cartTable.getColumns().addAll(nameCol, priceCol, quantityCol, totalCol);

        cartTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void loadCartData() {
        cartItems = FXCollections.observableArrayList();
      
        String query = "SELECT d.DonutID, d.DonutName, d.DonutPrice, c.Quantity, " +
                       "(d.DonutPrice * c.Quantity) AS Total " +
                       "FROM cart c " +
                       "JOIN msdonut d ON c.DonutID = d.DonutID " +
                       "WHERE c.UserID = ?";

        try (Connection conn = Connect.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, currentUserID);
            ResultSet rs = pstmt.executeQuery();

            double totalPrice = 0;
            while (rs.next()) {
                String donutID = rs.getString("DonutID");
                String name = rs.getString("DonutName");
                double price = rs.getDouble("DonutPrice");
                int quantity = rs.getInt("Quantity");
                double total = rs.getDouble("Total");

                cartItems.add(new CartItem(donutID, name, price, quantity, total));
                totalPrice += total;
            }

            cartTable.setItems(cartItems);
            totalPriceLabel.setText("Rp. " + totalPrice);

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Error", "Failed to load cart: " + e.getMessage());
        }
    }

    private int getNextTransactionIndex() {
        String query = "SELECT MAX(CAST(SUBSTRING(TransactionID, 3) AS UNSIGNED)) AS MaxIndex FROM transactionheader";
        try (Connection conn = Connect.connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("MaxIndex") + 1;
            }
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Error", "Failed to get next transaction index: " + e.getMessage());
        }
        return 1;
    }

    private void checkoutSelectedItems() {
        ObservableList<CartItem> selectedItems = cartTable.getSelectionModel().getSelectedItems();

        if (selectedItems == null || selectedItems.isEmpty()) {
            showAlert(AlertType.ERROR, "Error", "No items selected! Please select items to check out.");
            return;
        }

        int nextTransactionIndex = getNextTransactionIndex();
        String transactionID = String.format("TR%03d", nextTransactionIndex);

        String insertHeader = "INSERT INTO transactionheader (TransactionID, UserID) VALUES (?, ?)";
        String insertDetail = "INSERT INTO transactiondetail (TransactionID, DonutID, Quantity) VALUES (?, ?, ?)";
        String deleteSelectedCartItems = "DELETE FROM cart WHERE UserID = ? AND DonutID = ?";

        try (Connection conn = Connect.connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtHeader = conn.prepareStatement(insertHeader)) {
                pstmtHeader.setString(1, transactionID);
                pstmtHeader.setString(2, currentUserID);
                pstmtHeader.executeUpdate();
            }

            try (PreparedStatement pstmtDetail = conn.prepareStatement(insertDetail);
                 PreparedStatement pstmtDelete = conn.prepareStatement(deleteSelectedCartItems)) {

                for (CartItem item : selectedItems) {
                    pstmtDetail.setString(1, transactionID);
                    pstmtDetail.setString(2, item.getDonutID());
                    pstmtDetail.setInt(3, item.getQuantity());
                    pstmtDetail.addBatch();

                    pstmtDelete.setString(1, currentUserID);
                    pstmtDelete.setString(2, item.getDonutID());
                    pstmtDelete.addBatch();
                }

                pstmtDetail.executeBatch();
                pstmtDelete.executeBatch();
            }

            conn.commit();
            showAlert(AlertType.INFORMATION, "Success", "Selected items have been successfully checked out!");
            loadCartData();

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Error", "Checkout failed: " + e.getMessage());
        }
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

