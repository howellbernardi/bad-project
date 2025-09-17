package main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import util.Connect;
import java.sql.*;

public class Login extends Application {

    @Override
    public void start(Stage primaryStage) {
        LoginScene loginScene = new LoginScene(primaryStage);
        primaryStage.setScene(loginScene.getScene());
        primaryStage.setTitle("DvCO Donut Store");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
class LoginScene {
    private Scene scene;

    public LoginScene(Stage primaryStage) {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(20));


        MenuBar menuBar = createMenuBar(primaryStage, true);
        Label titleLabel = new Label("Login");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");


        VBox topContainer = new VBox(menuBar, titleLabel);
        topContainer.setAlignment(Pos.CENTER);
        layout.setTop(topContainer);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button loginButton = new Button("Login");

        form.add(new Label("Email:"), 0, 0);
        form.add(emailField, 1, 0);
        form.add(new Label("Password:"), 0, 1);
        form.add(passwordField, 1, 1);
        form.add(loginButton, 1, 2);

        layout.setCenter(form);

        loginButton.setOnAction(e -> {
            String email = emailField.getText();
            String password = passwordField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                showAlertError("Login Failed", "Please fill all fields.");
                return;
            }

            try (Connection conn = Connect.connect()) {
                if (conn != null) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT UserID, Role, Username FROM msuser WHERE Email = ? AND Password = ?");
                    stmt.setString(1, email);
                    stmt.setString(2, password);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String userID = rs.getString("UserID");
                        String role = rs.getString("Role");
                        String username = rs.getString("Username");

                        if ("Admin".equals(role)) {
                            showAlertInformation("Welcome Admin", "Hello, " + username);
                            Admin admin = new Admin();
                            primaryStage.setScene(admin.getAdminScene(primaryStage));
                        } else {
                            showAlertInformation("Welcome User", "Hello, " + username);
                            HomeScreenUser userHome = new HomeScreenUser();
                            primaryStage.setScene(userHome.getHomeScene(primaryStage, userID));
                        }
                    } else {
                        showAlertError("Login Failed", "Invalid credentials.");
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlertError("Database Error", "An error occurred.");
            }
        });

        this.scene = new Scene(layout, 800, 600);
    }

    public Scene getScene() {
        return scene;
    }

    private MenuBar createMenuBar(Stage primaryStage, boolean isLogin) {
        MenuBar menuBar = new MenuBar();
        Menu menu = new Menu("Menu");

        MenuItem loginItem = new MenuItem("Login");
        loginItem.setDisable(isLogin);
        loginItem.setOnAction(e -> primaryStage.setScene(new LoginScene(primaryStage).getScene()));

        MenuItem registerItem = new MenuItem("Register");
        registerItem.setDisable(!isLogin);
        registerItem.setOnAction(e -> primaryStage.setScene(new RegisterScene(primaryStage).getScene()));

        menu.getItems().addAll(loginItem, registerItem);
        menuBar.getMenus().add(menu);

        return menuBar;
    }

    private void showAlertError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlertInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}



class RegisterScene {
    private Scene scene;

    public RegisterScene(Stage primaryStage) {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(20));

        MenuBar menuBar = createMenuBar(primaryStage, false);

        Label titleLabel = new Label("Register");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        
        VBox topContainer = new VBox(menuBar, titleLabel);
        topContainer.setAlignment(Pos.CENTER);
        layout.setTop(topContainer);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        Spinner<Integer> ageSpinner = new Spinner<>(1, 100, 18);
        RadioButton maleRadio = new RadioButton("Male");
        RadioButton femaleRadio = new RadioButton("Female");
        ToggleGroup genderGroup = new ToggleGroup();
        maleRadio.setToggleGroup(genderGroup);
        femaleRadio.setToggleGroup(genderGroup);
        ComboBox<String> countryComboBox = new ComboBox<>();
        countryComboBox.getItems().addAll("Indonesia", "Malaysia", "Singapore");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        CheckBox termsCheckBox = new CheckBox("Agree to terms and conditions");
        Button registerButton = new Button("Register");

        form.add(new Label("Username:"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(new Label("Email:"), 0, 1);
        form.add(emailField, 1, 1);
        form.add(new Label("Password:"), 0, 2);
        form.add(passwordField, 1, 2);
        form.add(new Label("Confirm Password:"), 0, 3);
        form.add(confirmPasswordField, 1, 3);
        form.add(new Label("Age:"), 0, 4);
        form.add(ageSpinner, 1, 4);
        form.add(new Label("Gender:"), 0, 5);
        form.add(maleRadio, 1, 5);
        form.add(femaleRadio, 1, 6);
        form.add(new Label("Country:"), 0, 7);
        form.add(countryComboBox, 1, 7);
        form.add(new Label("Phone Number:"), 0, 8);
        form.add(phoneField, 1, 8);
        form.add(termsCheckBox, 1, 9);
        form.add(registerButton, 1, 10);

        layout.setCenter(form);

        registerButton.setOnAction(e -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            int age = ageSpinner.getValue();
            String gender = maleRadio.isSelected() ? "Male" : femaleRadio.isSelected() ? "Female" : null;
            String country = countryComboBox.getValue();
            String phone = phoneField.getText();
            boolean agreed = termsCheckBox.isSelected();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                gender == null || country == null || phone.isEmpty() || !agreed) {
                showAlertError("Registration Failed", "Please fill all fields and agree to the terms.");
                return;
            }

            if (!email.endsWith("@gmail.com")) {
                showAlertError("Registration Failed", "Email must end with '@gmail.com'.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showAlertError("Registration Failed", "Passwords do not match.");
                return;
            }

            if (age <= 13) {
                showAlertError("Registration Failed", "Age must be older than 13.");
                return;
            }

            try (Connection conn = Connect.connect()) {
                if (conn != null) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO msuser (UserID, Username, Email, Password, Age, Gender, Country, PhoneNumber, Role) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

                    Statement indexStmt = conn.createStatement();
                    ResultSet rs = indexStmt.executeQuery("SELECT COUNT(*) AS count FROM msuser");
                    int count = rs.next() ? rs.getInt("count") + 1 : 1;
                    String userId = String.format("US%03d", count);

                    stmt.setString(1, userId);
                    stmt.setString(2, username);
                    stmt.setString(3, email);
                    stmt.setString(4, password);
                    stmt.setInt(5, age);
                    stmt.setString(6, gender);
                    stmt.setString(7, country);
                    stmt.setString(8, phone);
                    stmt.setString(9, "User");

                    stmt.executeUpdate();

                    showAlertInformation("Registration Successful", "You can now log in.");
                    primaryStage.setScene(new LoginScene(primaryStage).getScene());
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlertError("Database Error", "An error occurred: " + ex.getMessage());
            }
        });

        this.scene = new Scene(layout, 800, 600);
    }

    public Scene getScene() {
        return scene;
    }

    private MenuBar createMenuBar(Stage primaryStage, boolean isLogin) {
        MenuBar menuBar = new MenuBar();
        Menu menu = new Menu("Menu");

        MenuItem loginItem = new MenuItem("Login");
        loginItem.setDisable(isLogin);
        loginItem.setOnAction(e -> primaryStage.setScene(new LoginScene(primaryStage).getScene()));

        MenuItem registerItem = new MenuItem("Register");
        registerItem.setDisable(!isLogin);
        registerItem.setOnAction(e -> primaryStage.setScene(new RegisterScene(primaryStage).getScene()));

        menu.getItems().addAll(loginItem, registerItem);
        menuBar.getMenus().add(menu);

        return menuBar;
    }

    private void showAlertError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlertInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

