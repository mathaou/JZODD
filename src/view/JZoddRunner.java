package view;

import control.JZoddController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class JZoddRunner extends Application{

	public static void main(String[]args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/jzodd.fxml"));
		loader.setClassLoader(getClass().getClassLoader());
		loader.setController(new JZoddController());
		Parent root = loader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		
		Font.loadFont(getClass().getResource("new_cinema_a.ttf").toExternalForm(), 12);
		scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
		
		primaryStage.setTitle("JZODD | MIDI Builder");
		
		primaryStage.setMaximized(false);
		primaryStage.show();
		
		primaryStage.setOnHiding(event -> {
			((JZoddController) loader.getController()).getES().shutdown();
			((JZoddController) loader.getController()).ps.close();
			System.exit(0);
		});
	}

}
