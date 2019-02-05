package view;

import control.JZoddController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import jfxtras.styles.jmetro8.JMetro;

/**
 * 
 * @author Matt Farstad
 * @version 1.0
 */
public class JZoddRunner extends Application{

	/**
	 * Main method to launch application.
	 * @param args
	 */
	public static void main(String[]args) {
		launch(args);
	}

	/**
	 * Start method to begin the visual part of app.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/jzodd.fxml"));
		loader.setClassLoader(getClass().getClassLoader());
		loader.setController(new JZoddController());
		Parent root = loader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		
		//file paths are a nightmare so I just shoved the stuff I needed into the same package.
		Font.loadFont(getClass().getResource("new_cinema_a.ttf").toExternalForm(), 12);
		scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
		new JMetro(JMetro.Style.LIGHT).applyTheme(root);
		
		primaryStage.setTitle("JZODD | MIDI Builder");
		
		primaryStage.setMaximized(false);
		primaryStage.show();
		
		//closes threads and printwriters on exit
		primaryStage.setOnHiding(event -> {
			((JZoddController) loader.getController()).getES().shutdown();
			((JZoddController) loader.getController()).ps.close();
			System.exit(0);
		});
	}

}
