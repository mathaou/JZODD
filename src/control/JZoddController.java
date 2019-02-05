package control;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.javafx.scene.control.skin.TextAreaSkin;

import driver.Groove;
import exceptions.LudicrousTempoException;
import exceptions.MalformedTimeSignatureException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import midi.MidiBuilder;
import midi.Progression;

/**
 * Controller object that handles a bunch o' stuff.
 * @author Matt Farstad
 * @version 1.0
 */
public class JZoddController implements Initializable {

	private ToggleGroup tg = new ToggleGroup();

	private String save = "";

	public StringBuilder out = new StringBuilder();

	public Boolean t = true;

	public ExecutorService e = Executors.newFixedThreadPool(4);

	public PrintStream ps;

	public JZoddController() {
	}

	@FXML
	private TextField bpmTextField, velocityTextField, num, den;

	@FXML
	private Button generateMidiButton;

	@FXML
	private ToggleButton readMe;

	@FXML
	private TextArea lRhythmTextArea, rRhythmTextArea, chordsTextArea;

	@FXML
	private RadioButton lBass;

	@FXML
	private Canvas previewCanvas;

	@FXML
	private AnchorPane root;

	@FXML
	private void generateMidi() {

		MidiBuilder.instance.getPlayEvents().clear();

		ExecutorService s = Executors.newFixedThreadPool(2);

		s.execute(new Runnable() {
			@Override
			public void run() {
				String chords = chordsTextArea.getText();
				String lRhythm = lRhythmTextArea.getText();
				String rRhythm = rRhythmTextArea.getText();

				chords = generateDuplicates(chords);

				lRhythm = generateDuplicates(lRhythm);
				rRhythm = generateDuplicates(rRhythm);

				Progression p = new Progression(chords);

				Groove g1 = new Groove(lBass.isSelected(), lRhythm);
				Groove g2 = new Groove(false, rRhythm);

				p.addGrooveChannel(g1);
				p.addGrooveChannel(g2);

				try {
					MidiBuilder.instance.setTempo(Integer.parseInt(bpmTextField.getText()));
					MidiBuilder.instance.setTimeSignature(Integer.parseInt(num.getText()),
							Integer.parseInt(den.getText()));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (LudicrousTempoException e) {
					e.printStackTrace();
				} catch (MalformedTimeSignatureException e) {
					e.printStackTrace();
				}

				p.generateMidi(Integer.parseInt(velocityTextField.getText()));
			}
		});

		s.shutdown();
	}

	private String generateDuplicates(String s) {
		StringBuilder sb = new StringBuilder(s);

		dupe(sb, "\\(([^\\)]+)\\)\\s*[xX]\\s*(\\d*)", s);
		dupe(sb, "(.*)\\s*[xX]\\s*(\\d*)", sb.toString());

		return sb.toString();
	}

	private void dupe(StringBuilder sb, String reg, String s) {
		String temp = "";

		Matcher group = Pattern.compile(reg).matcher(s);

		while (group.find()) {
			for (int i = 0; i < Integer.parseInt(group.group(2)); i++) {
				String clean = group.group(1);
				if (clean.charAt(0) == '(') {
					clean = clean.substring(1);
				}
				if (clean.charAt(clean.length() - 1) == ')') {
					clean = clean.substring(0, clean.length() - 1);
				}
				temp += clean + "\n";
			}
			int start = sb.indexOf(group.group(0));
			sb.replace(start, group.group(0).length() + start, temp);
			temp = "";
		}
	}

	@FXML
	private void readMeDisplay() {
		e.execute(new Runnable() {
			@Override
			public void run() {
				Scanner scan = new Scanner("[Chords]\r\n" + 
						"\r\n" + 
						" 	Chords can be maj, min, aug, or dim with up to two extensions from 6-13 \r\n" + 
						" (5-13 on the second/ third) on all the obvious points (no b12 or #8 sorry!) \r\n" + 
						" over any bass note. Exceptions are in place and will be output to limit\r\n" + 
						" malformed chord notation. Inversions are limited by number of notes in the \r\n" + 
						" chord, so C:^^^ is invalid but C7:^^^ is valid. X (a number) will add a line \r\n" + 
						" to the engine X times, and one layer of encapsulation has been tested and is \r\n" + 
						" functioning.\r\n" + 
						" \r\n" + 
						"	- (Ebmaj7/Ab:4^^^ | Gbmin6b5#13/E:7^^ | x 2) x 2\r\n" + 
						"  \r\n" + 
						" [Groove]\r\n" + 
						" \r\n" + 
						" 	Uppercase letters == rest, lowercase letters == notes. Possible values are \r\n" + 
						" [seqhwSEQHW] for sixteenth, eight, quarter, half, and whole respectively. \r\n" + 
						" Note values can be concatenated, but only of same case. A [:] denotes a\r\n" + 
						" chord change and should be used accordingly. Repeats work on groove too\r\n" + 
						" and should be used to match any repeats on chords! \r\n" + 
						" \r\n" + 
						"	- (qwwwwwh:q+e+s+eww: x 2) x 2\r\n" + 
						"  \r\n" + 
						"Number of chord changes and number of chords must be equal.");
				if (readMe.isSelected()) {
					while (scan.hasNextLine()) {
						save += scan.nextLine() + "\n";
					}
					previewCanvas.getGraphicsContext2D().setFont(new Font(
							GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()[7].getFontName(), 12));
					previewCanvas.getGraphicsContext2D().fillText(save, 2, 12);
				} else {
					previewCanvas.getGraphicsContext2D().clearRect(0, 0, previewCanvas.getWidth(),
							previewCanvas.getHeight());
					save = "";
				}
			}
		});
	}

	private void rRhythmListeners() {
		rRhythmTextArea.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				
			}
		});
		
		rRhythmTextArea.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (out.length() > 0) {
					out.setLength(0);
					t = true;
					previewCanvas.getGraphicsContext2D().clearRect(0, 0, previewCanvas.getWidth(),
							previewCanvas.getHeight());
				}

				if (event.getCode().equals(KeyCode.TAB)) {
					Node node = (Node) event.getSource();

					if (node instanceof TextArea) {
						TextAreaSkin skin = (TextAreaSkin) ((TextArea) node).getSkin();

						if (event.isShiftDown()) {
							skin.getBehavior().traversePrevious();
						} else {
							skin.getBehavior().traverseNext();
						}
					}

					event.consume();
				}
			}
		});
	}

	private void lRhythmListeners() {
		lRhythmTextArea.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (out.length() > 0) {
					out.setLength(0);
					t = true;
					previewCanvas.getGraphicsContext2D().clearRect(0, 0, previewCanvas.getWidth(),
							previewCanvas.getHeight());
				}

				if (event.getCode().equals(KeyCode.TAB)) {
					Node node = (Node) event.getSource();

					if (node instanceof TextArea) {
						TextAreaSkin skin = (TextAreaSkin) ((TextArea) node).getSkin();

						if (event.isShiftDown()) {
							skin.getBehavior().traversePrevious();
						} else {
							skin.getBehavior().traverseNext();
						}
					}

					event.consume();
				}
			}
		});
	}

	private void chordsListeners() {
		chordsTextArea.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.TAB)) {
					Node node = (Node) event.getSource();

					if (node instanceof TextArea) {
						TextAreaSkin skin = (TextAreaSkin) ((TextArea) node).getSkin();

						if (event.isShiftDown()) {
							skin.getBehavior().traversePrevious();
						} else {
							skin.getBehavior().traverseNext();
						}
					}

					event.consume();
				}

				if (event.getCode().equals(KeyCode.SPACE)) {
					Node node = (Node) event.getSource();

					if (node instanceof TextArea) {
						TextArea temp = (TextArea) node;
						if (temp.getText().trim().length() > 0) {
							if (temp.getText().charAt(temp.getText().length() - 1) != ')'
									&& temp.getText().charAt(temp.getText().length() - 1) != 'x'
									&& temp.getText().charAt(temp.getText().length() - 1) != 'X') {
								temp.appendText(" |");
								temp.positionCaret(temp.getText().length());
							}
						}
					}

					event.consume();
				}

			}
		});
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		Console console = new Console(previewCanvas);
		ps = new PrintStream(console, true);
		System.setErr(ps);

		Runnable r = new Runnable() {
			@Override
			public void run() {

				rRhythmListeners();

				lRhythmListeners();

				chordsListeners();
			}
		};

		Platform.runLater(r);
	}

	private class Console extends OutputStream {

		public Canvas c;

		public Console(Canvas c) {
			this.c = c;
		}

		private int closestSpace(String s, int x) {
			int num = 0;
			if (x < s.length() - 50) {
				String i = s.substring(x, x + 50);
				num = i.lastIndexOf(" ") + x;
			} else {
				String i = s.substring(x);
				num = i.lastIndexOf(" ") + x;
			}
			return num + 1;
		}

		@Override
		public void write(int i) throws IOException {
			if (out.toString().length() <= 200 && t) {
				out.append(String.valueOf((char) i));
			}
			if (out.toString().length() > 200 && t) {
				String sub = out.toString().substring(0, out.toString().indexOf("at "));
				out.delete(sub.length(), out.length());
				int range = out.toString().lastIndexOf("Exception:") + 11;
				out.insert(range, "\n");
				for (int x = range; x < out.toString().length(); x += 100) {
					out.insert(closestSpace(out.toString(), x), "\n");
				}
				c.getGraphicsContext2D().fillText(out.toString().trim(), 2, 12);
				t = false;
			}
		}
	}

	public ExecutorService getES() {
		return this.e;
	}

}
