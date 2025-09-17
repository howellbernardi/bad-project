package main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import util.Connect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Donut;

public class Admin {
    private TableView<Donut> tableView;
    private ObservableList<Donut> donutList = FXCollections.observableArrayList();
    private Connection con;
    
    private TextField tfName;
    private TextArea taDescription;
    private TextField tfPrice;
    
    Scene scene;

    public Scene getAdminScene(Stage stage) {
        con = Connect.connect(); 
        
        HBox header = new HBox();
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("Logout");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> stage.setScene(new LoginScene(stage).getScene())); 
        menuFile.getItems().add(logoutItem); 
        menuBar.getMenus().add(menuFile);    

        Label lblWelcome = new Label("Hello, admin");
        lblWelcome.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        header.getChildren().addAll(lblWelcome);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);
        header.setPadding(new Insets(10, 20, 10, 20));

        tableView = new TableView<>();
        TableColumn<Donut, String> colID = new TableColumn<>("Donut ID");
        colID.setCellValueFactory(new PropertyValueFactory<>("id"));
        colID.setPrefWidth(100);

        TableColumn<Donut, String> colName = new TableColumn<>("Donut Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);

        TableColumn<Donut, String> colDescription = new TableColumn<>("Donut Description");
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDescription.setPrefWidth(300);

        TableColumn<Donut, Integer> colPrice = new TableColumn<>("Donut Price");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setPrefWidth(100);

        tableView.getColumns().addAll(colID, colName, colDescription, colPrice);
        loadDonuts(); 
        
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setOnMouseClicked(e -> {
            Donut selectedDonut = tableView.getSelectionModel().getSelectedItem();
            if (selectedDonut != null) {
                tfName.setText(selectedDonut.getName());
                taDescription.setText(selectedDonut.getDescription());
                tfPrice.setText(String.valueOf(selectedDonut.getPrice()));
            }
        });

        VBox form = new VBox(10);
        form.setPadding(new Insets(20, 10, 10, 10));

        Label lblName = new Label("Donut Name");
        tfName = new TextField();
        tfName.setMaxWidth(400);

        Label lblDescription = new Label("Donut Description");
        taDescription = new TextArea();
        taDescription.setMaxWidth(400);
        taDescription.setPrefHeight(60);

        Label lblPrice = new Label("Donut Price");
        tfPrice = new TextField();
        tfPrice.setMaxWidth(400);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        Button btnAdd = new Button("Add Donut");
        btnAdd.setOnAction(e -> addDonut(tfName, taDescription, tfPrice));
        
        Button btnUpdate = new Button("Update Donut");
        btnUpdate.setOnAction(e -> updateDonut(tfName, taDescription, tfPrice));
        
        Button btnDelete = new Button("Delete Donut");
        btnDelete.setOnAction(e -> deleteDonut());

        buttonBox.getChildren().addAll(btnAdd, btnUpdate, btnDelete);
        form.getChildren().addAll(lblName, tfName, lblDescription, taDescription, lblPrice, tfPrice, buttonBox);

        VBox root = new VBox(10);
        root.setPadding(new Insets(10, 20, 20, 20));
        root.getChildren().addAll(menuBar, header, new Label("Active Donut:"), tableView, form);

        scene = new Scene(root, 800, 600);
        return scene;
    }
    
    private void loadDonuts() {
        donutList.clear();
        String query = "SELECT * FROM msdonut";
        try (PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("DonutID");
                String name = rs.getString("DonutName");
                String description = rs.getString("DonutDescription");
                int price = rs.getInt("DonutPrice");

                donutList.add(new Donut(id, name, description, price));
            }
            tableView.setItems(donutList);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load data from database.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private void addDonut(TextField tfName, TextArea taDescription, TextField tfPrice) {
        String name = tfName.getText();
        String description = taDescription.getText();
        String priceText = tfPrice.getText();

        
        if (name.isEmpty()) {
            showErrorAlert("Donut name cannot be empty!");
            return;
        }
        if (description.isEmpty()) {
            showErrorAlert("Donut description cannot be empty!");
            return;
        }
        if (priceText.isEmpty()) {
            showErrorAlert("Donut price cannot be empty!");
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceText);
        } catch (NumberFormatException e) {
            showErrorAlert("Donut price must be a valid number!");
            return;
        }

        
        String query = "INSERT INTO msdonut (DonutID, DonutName, DonutDescription, DonutPrice) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            String donutID = generateDonutID(); 
            pst.setString(1, donutID);
            pst.setString(2, name);
            pst.setString(3, description);
            pst.setInt(4, price);

            pst.executeUpdate();
            loadDonuts(); 
            clearForm(tfName, taDescription, tfPrice);
            showInfoAlert("Donut Added Successfully", "Donut Added Successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Failed to add donut to database.");
        }
    }
    
    private String generateDonutID() {
        int nextID = donutList.size() + 1; 
        return String.format("DN%03d", nextID);}
    
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Request");
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Message");
        alert.setContentText(message);
        alert.showAndWait();
    }

    
    private void clearForm(TextField tfName, TextArea taDescription, TextField tfPrice) {
        tfName.clear();
        taDescription.clear();
        tfPrice.clear();
    }


    private void updateDonut(TextField tfName, TextArea taDescription, TextField tfPrice) {
        Donut selectedDonut = tableView.getSelectionModel().getSelectedItem();

        
        if (selectedDonut == null) {
            showErrorAlert("Please select a donut to update!");
            return;
        }

        String name = tfName.getText();
        String description = taDescription.getText();
        String priceText = tfPrice.getText();

        
        if (name.isEmpty()) {
            showErrorAlert("Donut name cannot be empty!");
            return;
        }
        if (description.isEmpty()) {
            showErrorAlert("Donut description cannot be empty!");
            return;
        }
        if (priceText.isEmpty()) {
            showErrorAlert("Donut price cannot be empty!");
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceText);
        } catch (NumberFormatException e) {
            showErrorAlert("Donut price must be a valid number!");
            return;
        }

        
        String query = "UPDATE msdonut SET DonutName = ?, DonutDescription = ?, DonutPrice = ? WHERE DonutID = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, name);
            pst.setString(2, description);
            pst.setInt(3, price);
            pst.setString(4, selectedDonut.getId());

            pst.executeUpdate();
            loadDonuts(); 
            clearForm(tfName, taDescription, tfPrice);
            showInfoAlert("Donut Updated Successfully", "Donut Updated Successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Failed to update donut in database.");
        }
    }


    private void deleteDonut() {
        Donut selectedDonut = tableView.getSelectionModel().getSelectedItem();

        
        if (selectedDonut == null) {
            showErrorAlert("Please select a donut to delete!");
            return;
        }

        
        String query = "DELETE FROM msdonut WHERE DonutID = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, selectedDonut.getId());

            pst.executeUpdate();
            loadDonuts(); 
            clearForm(tfName, taDescription, tfPrice);
            showInfoAlert("Donut Deleted Successfully", "Donut Deleted Successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Failed to delete donut from database.");
        }
    }
}
