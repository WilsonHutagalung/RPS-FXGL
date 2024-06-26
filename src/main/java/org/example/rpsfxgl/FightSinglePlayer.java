package org.example.rpsfxgl;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

public class FightSinglePlayer {

    @FXML
    private ProgressBar barHealthCom;

    @FXML
    private ProgressBar barHealthPlayer;

    @FXML
    private Button btnPaper;

    @FXML
    private Button btnRock;

    @FXML
    private Button btnScissor;

    @FXML
    private Label lblComName;

    @FXML
    private Label lblPlayerName;

    @FXML
    private Label lblStage;

    private Computer currentComputer;
    private Akun currentPlayer;

    private int comHp;
    private int comDamage;

    private int playerHp;
    private int playerDamage;

    private final String[] choices = {"Rock", "Paper", "Scissors"};
    private Random random = new Random();

    private ArrayList<Byte> gamepadState = new ArrayList<>();


    public void setFightInfo(Computer computer, Akun player) {
        currentComputer = computer;
        currentPlayer = player;
        playerHp = player.getHealth();
        playerDamage = player.getDamage();
        comHp = computer.getComHp();

        lblStage.setText("Stage " + computer.getComStage());
        lblComName.setText(computer.getComName());
        lblPlayerName.setText(player.getUsername());
        barHealthCom.setProgress((double) comHp / 100);
        barHealthPlayer.setProgress(1.0);

        Platform.runLater(() -> {
            Runnable gamepadStateRunnable = () -> {
                if (!GLFW.glfwInit()) {
                    System.err.println("Failed to initialize GLFW");
                    System.exit(-1);
                }

                GLFWGamepadState state = GLFWGamepadState.create();

                while (true) {
                    if (GLFW.glfwGetGamepadState(GLFW.GLFW_JOYSTICK_1, state)) {
                        gamepadState.clear();
                        for (int i = 0; i <= 3; i++) {
                            gamepadState.add(state.buttons(i));
                        }

                        Platform.runLater(() -> {
                            if (!gamepadState.isEmpty()) {
                                System.out.println("Gamepad State: " + gamepadState);
                                if (gamepadState.get(0) == 1) {
                                    handlePlayerChoice("Rock", currentComputer);
                                } else if (gamepadState.get(1) == 1) {
                                    handlePlayerChoice("Paper", currentComputer);
                                } else if (gamepadState.get(2) == 1) {
                                    handlePlayerChoice("Scissors", currentComputer);
                                }
                            }
                        });
                    }

                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    GLFW.glfwPollEvents();
                }
            };

            Thread gamepadThread = new Thread(gamepadStateRunnable);
            gamepadThread.setDaemon(true);
            gamepadThread.start();
        });

        btnRock.setOnAction(actionEvent -> handlePlayerChoice("Rock", computer));
        btnPaper.setOnAction(actionEvent -> handlePlayerChoice("Paper", computer));
        btnScissor.setOnAction(actionEvent -> handlePlayerChoice("Scissors", computer));
    }

    private void handlePlayerChoice(String playerChoice, Computer computer) {
        String comChoice = choices[random.nextInt(choices.length)];
        lblStage.setText("Player: " + playerChoice + " vs " + "Computer: " + comChoice);

        if (playerChoice.equals(comChoice)) {
            lblStage.setText(lblStage.getText() + " -> Draw!");
        } else if ((playerChoice.equals("Rock") && comChoice.equals("Scissors")) ||
                (playerChoice.equals("Paper") && comChoice.equals("Rock")) ||
                (playerChoice.equals("Scissors") && comChoice.equals("Paper"))) {
            int newComHp = computer.getComHp() - playerDamage;
            computer.setComHp(newComHp);
            barHealthCom.setProgress((double) newComHp / 100);
            lblStage.setText(lblStage.getText() + " -> Player Wins!");

            if (newComHp <= 0) {
                lblStage.setText("You win the game!");
                disableButtons();
                handlePlayerWin();
            }
        } else {
            int newPlayerHp = playerHp - computer.getComDamage();
            playerHp = newPlayerHp;
            barHealthPlayer.setProgress((double) newPlayerHp / 100);
            lblStage.setText(lblStage.getText() + " -> Computer Wins!");

            if (newPlayerHp <= 0) {
                lblStage.setText("You lose the game!");
                disableButtons();
                showEndGameDialog("You lose the game!");
            }
        }
    }


    private void disableButtons() {
        btnRock.setDisable(true);
        btnPaper.setDisable(true);
        btnScissor.setDisable(true);
    }

    private void showEndGameDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(message);
        alert.setContentText("Choose your option.");

        ButtonType buttonRetry = new ButtonType("Retry");
        ButtonType buttonBack = new ButtonType("Back to List");

        alert.getButtonTypes().setAll(buttonRetry, buttonBack);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == buttonRetry) {
            retryGame();
        } else {
            backToList();
        }
    }

    private void handlePlayerWin() {
        int currentExp = currentPlayer.getXp();
        currentExp += currentComputer.getComXp();
        currentPlayer.setXp(currentExp);

        if (currentExp >= 100) {
            int currentLevel = currentPlayer.getLevel();
            currentLevel++;
            currentPlayer.setLevel(currentLevel);
            currentPlayer.setHealth(currentPlayer.getHealth() + 10);
            currentPlayer.setXp(currentExp - 100);
            lblStage.setText("Player wins and leveled up to level " + currentLevel);
        }

        currentPlayer.updatePlayerData();
        showEndGameDialog("You win the game!");
    }

    private void retryGame() {
        currentComputer.setComHp(comHp);

        playerHp = currentPlayer.getHealth();
        barHealthCom.setProgress((double) comHp / 100);
        barHealthPlayer.setProgress(1.0);
        lblStage.setText("Stage " + currentComputer.getComStage());
        btnRock.setDisable(false);
        btnPaper.setDisable(false);
        btnScissor.setDisable(false);
    }





    private void backToList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("listComputer.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("List Computer");
            stage.setScene(scene);

            ListComputerController controller = loader.getController();
            controller.setComputerData(Arena.getComputerDataForStage(currentComputer.getComStage()));

            Stage currentStage = (Stage) lblStage.getScene().getWindow();
            currentStage.close();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
