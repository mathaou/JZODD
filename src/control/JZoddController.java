package control;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.sun.javafx.scene.control.skin.TextAreaSkin;

import driver.Groove;
import driver.Label;
import driver.Project;
import driver.TimeEvent;
import exceptions.LudicrousTempoException;
import exceptions.MalformedTimeSignatureException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import midi.MidiBuilder;
import midi.Progression;
import view.JZoddRunner;

/**
 * Controller object that handles a bunch o' stuff.
 * 
 * @author Matt Farstad
 * @version 1.0
 */
public class JZoddController implements Initializable {

	/*
	 * LOCAL VARIABLES
	 */

	private ToggleGroup tg = new ToggleGroup();

	private String save = "";

	public StringBuilder out = new StringBuilder();

	public Boolean t = true;

	public ExecutorService e = Executors.newFixedThreadPool(4);

	public PrintStream ps;

	private int var = 10, skip = 80, lY = 50;

	private FileUtils util = new FileUtils();

	private File saveDir = new File("save.tz"), projectDir;

	private ArrayList<Label> saves;

	/*
	 * CALLED BEFORE INITIALIZE AND INJECTION
	 */

	public JZoddController() {
	}

	/*
	 * INJECTED OBJECTS
	 */

	@FXML
	private TextField bpmTextField, velocityTextField, num, den;

	@FXML
	private Button generateMidiButton, saveLabel;

	@FXML
	private ToggleButton readMe;

	@FXML
	private TextArea lRhythmTextArea, rRhythmTextArea, chordsTextArea, labelEditor;

	@FXML
	private RadioButton lBass;

	@FXML
	private Canvas previewCanvas;

	@FXML
	private AnchorPane root, mainArea;

	@FXML
	private VBox sidebar;

	@FXML
	private ComboBox<String> labelChooser;

	@FXML
	private Menu file, help;

	/*
	 * INJECTED METHODS
	 */

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

	@FXML
	private void readMeDisplay() {

		if (readMe.isSelected()) {
			root.getScene().getWindow().setWidth(1250);
			sidebar.setManaged(true);
			sidebar.setVisible(true);
		} else {
			root.getScene().getWindow().setWidth(820);
			sidebar.setManaged(false);
			sidebar.setVisible(false);
		}
	}

	@FXML
	private void comboAction(ActionEvent e) {
		Label l = getLabelForKey(labelChooser.getValue());

		labelEditor.setText(l.toString());
	}

	@FXML
	private void save() {
		String temp = labelEditor.getText();

		Matcher labels = Pattern.compile("\\w*:\\s?[^\\}]*\\}").matcher(temp);

		while (labels.find()) {
			Matcher match = Pattern.compile("\\w*(?=:)").matcher(labels.group(0).trim());

			match.find();

			String header = match.group(0);

			match = Pattern.compile("\\{[^\\}]*\\}").matcher(labels.group(0).trim());

			match.find();

			String body = match.group(0).replaceAll("\\{|\\}", "");

			Label save = new Label(header, body);

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {
						if (saveDir.isFile()) {
							FileInputStream fi = new FileInputStream(saveDir);
							ObjectInputStream oi = new ObjectInputStream(fi);

							saves = (ArrayList<Label>) oi.readObject();

							for (Label l : saves) {
								System.out.println(l.toString());
							}

							fi.close();
							oi.close();
						}

						if (saves == null) {
							saves = new ArrayList<Label>();
						}

						saves.add(save);

						FileOutputStream fs = new FileOutputStream(saveDir);
						ObjectOutputStream os = new ObjectOutputStream(fs);

						os.writeObject(saves);

						fs.close();
						os.close();

						populateComboBox();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}

			});
		}
	}

	@FXML
	private void openGithub() {
		try {
			Desktop desktop = java.awt.Desktop.getDesktop();
			URI oURL = new URI("https://github.com/mathaou/JZODD/");
			desktop.browse(oURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void newProject() {
		chordsTextArea.clear();
		lRhythmTextArea.clear();
		rRhythmTextArea.clear();
	}

	@FXML
	private void saveProject() {
		projectDir = selectSaveFile();
		Project p = new Project(chordsTextArea.getText(), lRhythmTextArea.getText(), rRhythmTextArea.getText());
		try {
			FileOutputStream fs = new FileOutputStream(projectDir);
			ObjectOutputStream os = new ObjectOutputStream(fs);

			os.writeObject(p);

			fs.close();
			os.close();
			
			JZoddRunner.getStage().setTitle(String.format("JZODD [%s] | MIDI Builder", projectDir.getName()));
		} catch (Exception e) {
		}
	}

	@FXML
	private void openProject() {
		File o = selectOpenFile();
		projectDir = o;
		try {
			if (o.isFile()) {
				FileInputStream fi = new FileInputStream(o);
				ObjectInputStream oi = new ObjectInputStream(fi);

				Project p = (Project) oi.readObject();

				chordsTextArea.setText(p.getChords());
				lRhythmTextArea.setText(p.getlRhythm());
				rRhythmTextArea.setText(p.getrRhtyhm());

				fi.close();
				oi.close();
				
				JZoddRunner.getStage().setTitle(String.format("JZODD [%s] | MIDI Builder", o.getName()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * HELPER METHODS
	 */
	
	private void macroSave() {
		Project p = new Project(chordsTextArea.getText(), lRhythmTextArea.getText(), rRhythmTextArea.getText());
		try {
			FileOutputStream fs = new FileOutputStream(projectDir);
			ObjectOutputStream os = new ObjectOutputStream(fs);

			os.writeObject(p);

			fs.close();
			os.close();
			/*
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Calendar cal = Calendar.getInstance();
			
			KeyFrame a = new KeyFrame(Duration.seconds(3), evt -> JZoddRunner.getStage().setTitle(String.format("Saved %s at %s", projectDir.getName(), dateFormat.format(cal.getTime()))));
			KeyFrame b = new KeyFrame(Duration.seconds(0), evt -> JZoddRunner.getStage().setTitle(String.format("JZODD [%s] | MIDI Builder", projectDir.getName())));
			
			Timeline line = new Timeline(a,b);
			
			line.setCycleCount(0);

			e.execute(new Runnable() {
				@Override
				public void run() {
					line.play();
				}
			});*/
		} catch (Exception e) {
		}
	}

	private File selectSaveFile() {
		FileChooser choose = new FileChooser();

		FileChooser.ExtensionFilter ext = new FileChooser.ExtensionFilter("JZODD Project Files (*.jzp)", "*.jzp");

		choose.getExtensionFilters().add(ext);

		File f = choose.showSaveDialog(null);

		return f;
	}

	private File selectOpenFile() {
		FileChooser choose = new FileChooser();

		FileChooser.ExtensionFilter ext = new FileChooser.ExtensionFilter("JZODD Project Files (*.jzp)", "*.jzp");

		choose.getExtensionFilters().add(ext);

		File f = choose.showOpenDialog(null);

		return f;
	}

	private void populateComboBox() {
		e.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (saveDir.isFile()) {
						FileInputStream fi = new FileInputStream(saveDir);
						ObjectInputStream oi = new ObjectInputStream(fi);

						saves = (ArrayList<Label>) oi.readObject();
						ArrayList<String> temp = new ArrayList<String>();

						for (Label l : saves) {
							temp.add(l.getHeader());
						}

						labelChooser.setItems(FXCollections.observableArrayList(temp));

						fi.close();
						oi.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
	}

	private Label getLabelForKey(String key) {
		try {
			if (saveDir.isFile()) {
				FileInputStream fi = new FileInputStream(saveDir);
				ObjectInputStream oi = new ObjectInputStream(fi);

				saves = (ArrayList<Label>) oi.readObject();

				for (Label l : saves) {
					if (key.equals(l.getHeader())) {
						return l;
					}
				}

				fi.close();
				oi.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private void drawRectangle(GraphicsContext gc, Rectangle rect, Color c) {
		gc.setFill(c);
		gc.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
		gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
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
			try {
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
			} catch (Exception e) {

			}
		}
	}

	private void updateCanvas() {
		previewCanvas.getGraphicsContext2D().clearRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());

		String chords = generateDuplicates(chordsTextArea.getText());

		String rRhythm = generateDuplicates(rRhythmTextArea.getText());
		String lRhythm = generateDuplicates(lRhythmTextArea.getText());

		Progression p = new Progression(chords);
		Groove rGroove = new Groove(false, rRhythm);
		Groove lGroove = new Groove(lBass.isSelected(), lRhythm);

		int rSum = 0, lSum = 0;
		int rY = 10;
		int rCount = -1, lCount = -1;

		int grooveSumR = rGroove.getTimeEvents().stream().mapToInt(m -> Math.abs(m.getTimeEvent())).sum();
		int grooveSumL = lGroove.getTimeEvents().stream().mapToInt(m -> Math.abs(m.getTimeEvent())).sum();

		boolean noText = false;

		if (grooveSumR > 60 * 64 | grooveSumL > 60 * 64) {
			setVar(3);
			setSkip(30);
			setLY(25);
			noText = true;
		} else if (grooveSumR > 42 * 64 | grooveSumL > 42 * 64) {
			setVar(5);
			setSkip(50);
			setLY(35);
		} else if (grooveSumR > 36 * 64 | grooveSumL > 36 * 64) {
			setVar(7);
			setSkip(70);
			setLY(40);
		} else if (grooveSumR <= 36 * 64 && grooveSumL <= 36 * 64) {
			setVar(10);
			setSkip(80);
			setLY(50);
		}

		handleCanvasForHand(Color.web("#58A557"), Color.TRANSPARENT, Color.BLACK, lGroove, lSum, lY, var, lCount, p,
				skip, noText);
		handleCanvasForHand(Color.web("#B93D47"), Color.TRANSPARENT, Color.BLACK, rGroove, rSum, rY, var, rCount, p,
				skip, noText);
	}

	private void handleCanvasForHand(Color note, Color rest, Color text, Groove rGroove, int rSum, int rY, int var,
			int rCount, Progression p, int skip, boolean noText) {
		for (TimeEvent i : rGroove.getTimeEvents()) {

			int mod = (i.getTimeEvent() < 0) ? -1 : 1;

			int temp = i.getTimeEvent() * mod;

			if (temp == 0) {
				Rectangle r = new Rectangle(5 + rSum, 20 + rY, 5, var);
				drawRectangle(previewCanvas.getGraphicsContext2D(), r, Color.GREY);
				rCount++;
			} else {

				if ((rSum + temp) / 400 > 0) {
					rSum = 0;
					rY += skip;
				}

				Rectangle r = new Rectangle(10 + rSum, 20 + rY, temp, var);

				if (mod > 0) {
					drawRectangle(previewCanvas.getGraphicsContext2D(), r, note);
				} else {
					drawRectangle(previewCanvas.getGraphicsContext2D(), r, rest);
				}

				if (!noText) {
					previewCanvas.getGraphicsContext2D().setFill(Color.BLACK);
					previewCanvas.getGraphicsContext2D().fillText(temp + "", 5 + rSum, 15 + rY);
				}
				if (rCount == -1) {
					rCount++;
					if (p.getProgressionList().size() > 0 && rCount < p.getProgressionList().size()) {
						String t = "";
						if (temp > Groove.HALF) {
							if (p.getProgressionList().get(rCount).getBaseChord().length() > 6) {
								t = p.getProgressionList().get(rCount).getBaseChord().substring(0, 6) + "...";
							} else {
								t = p.getProgressionList().get(rCount).getBaseChord();
							}
						} else if (temp == Groove.HALF) {
							if (p.getProgressionList().get(rCount).getBaseChord().length() > 3) {
								t = p.getProgressionList().get(rCount).getRoot() + "...";
							} else {
								t = p.getProgressionList().get(rCount).getRoot();
							}
						} else {

						}

						if (!noText) {
							previewCanvas.getGraphicsContext2D().setFill(text);
							previewCanvas.getGraphicsContext2D().fillText(t, 20 + rSum, 15 + rY);
						}
					}
				} else {
					if (p.getProgressionList().size() > 0 && rCount < p.getProgressionList().size()) {
						String t = "";
						if (temp > Groove.HALF) {
							if (p.getProgressionList().get(rCount).getBaseChord().length() > 6) {
								t = p.getProgressionList().get(rCount).getBaseChord().substring(0, 6) + "...";
							} else {
								t = p.getProgressionList().get(rCount).getBaseChord();
							}
						} else if (temp == Groove.HALF) {
							if (p.getProgressionList().get(rCount).getBaseChord().length() > 3) {
								t = p.getProgressionList().get(rCount).getRoot() + "...";
							} else {
								t = p.getProgressionList().get(rCount).getRoot();
							}
						} else {

						}

						if (!noText) {
							previewCanvas.getGraphicsContext2D().setFill(text);
							previewCanvas.getGraphicsContext2D().fillText(t, 20 + rSum, 15 + rY);
						}
					}
				}
			}
			rSum += temp;
		}
	}

	/*
	 * LISTENERS
	 */

	private void rRhythmListeners() {
		rRhythmTextArea.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						updateCanvas();
					}
				});
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
		lRhythmTextArea.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						updateCanvas();
					}
				});
			}

		});

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

	private void checkNums() {
		bpmTextField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					bpmTextField.setText(newValue.replaceAll("[^\\d]", ""));
				}
			}
		});

		velocityTextField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					velocityTextField.setText(newValue.replaceAll("[^\\d]", ""));
				}
			}
		});

		den.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					den.setText(newValue.replaceAll("[^\\d]", ""));
				}
			}
		});

		num.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					num.setText(newValue.replaceAll("[^\\d]", ""));
				}
			}
		});
	}

	private void chordsListeners() {
		chordsTextArea.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						updateCanvas();
					}
				});
			}

		});

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

	/*
	 * CALLED AFTER INJECTION
	 */

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		sidebar.setManaged(false);
		sidebar.setVisible(false);

		Console console = new Console(previewCanvas);
		ps = new PrintStream(console, true);
		//System.setErr(ps);

		populateComboBox();

		Runnable r = new Runnable() {
			@Override
			public void run() {

				root.getScene().getWindow().setWidth(820);

				rRhythmListeners();

				lRhythmListeners();

				chordsListeners();

				checkNums();
				
				root.getScene().addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
					final KeyCombination s = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_ANY);
					@Override
					public void handle(KeyEvent arg0) {
						if(s.match(arg0)) {
							macroSave();
						}
					}});
			}
		};

		Platform.runLater(r);
	}

	/*
	 * REDIRECT ERROR OUTPUT TO CANVAS
	 */

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
				t = false;
			}
			if (!t) {
				previewCanvas.getGraphicsContext2D().clearRect(0, 0, previewCanvas.getWidth(),
						previewCanvas.getHeight());
				c.getGraphicsContext2D().fillText(out.toString().trim(), 2, 12);
			}
		}
	}

	/*
	 * GETTERS AND SETTERS
	 */

	public ExecutorService getES() {
		return this.e;
	}

	public int getVar() {
		return var;
	}

	public int getSkip() {
		return skip;
	}

	public int getLY() {
		return this.lY;
	}

	public void setVar(int var) {
		this.var = var;
	}

	public void setSkip(int skip) {
		this.skip = skip;
	}

	public void setLY(int lY) {
		this.lY = lY;
	}

}
