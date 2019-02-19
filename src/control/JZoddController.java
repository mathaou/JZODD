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
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

import org.apache.commons.io.FileUtils;

import com.sun.javafx.scene.control.skin.TextAreaSkin;

import driver.Groove;
import driver.Label;
import driver.Project;
import driver.RectanglePairing;
import driver.TimeEvent;
import exceptions.LudicrousTempoException;
import exceptions.MalformedTimeSignatureException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
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

	private File saveDir = new File("save.tz"), projectDir = null;

	private ArrayList<Label> saves = new ArrayList<Label>();
	
	private ArrayList<RectanglePairing<Color, Rectangle>> tooltips = new ArrayList<RectanglePairing<Color, Rectangle>>();

	private Sequencer midi;
	
	private Synthesizer synth;
	
	private int instrument = 0;

	/*
	 * CALLED BEFORE INITIALIZE AND INJECTION
	 */

	public JZoddController() {
		try {
			this.midi = MidiSystem.getSequencer();
			this.synth = MidiSystem.getSynthesizer();
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
	}

	/*
	 * INJECTED OBJECTS
	 */

	@FXML
	private TextField bpmTextField, velocityTextField, num, den;

	@FXML
	private Button generateMidiButton, saveLabel, delete;

	@FXML
	private ToggleButton readMe, loop;

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
	private ComboBox<String> labelChooser, instrumentBox;

	@FXML
	private Menu file, help;

	@FXML
	private javafx.scene.control.Label chordsLabel, rRhythmLabel, lRhythmLabel;

	/*
	 * INJECTED METHODS
	 */

	@FXML
	private synchronized void generateMidi() {

		MidiBuilder.instance.getPlayEvents().clear();

		ExecutorService s = Executors.newFixedThreadPool(2);

		s.execute(new Runnable() {
			@Override
			public void run() {
				Progression p = assembleProgression();

				p.generateMidi(Integer.parseInt(velocityTextField.getText()));
			}
		});

		s.shutdown();
	}

	@FXML
	private synchronized void deleteLabel() {
		String l = labelChooser.getValue();
		Label label = getLabelForKey(l);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					saves.remove(label);

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
				}
			}

		});
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
		try {
			Label l = getLabelForKey(labelChooser.getValue());
			labelEditor.setText(l.toString());
		} catch (Exception n) {

		}
	}

	@FXML
	private synchronized void save() {
		String temp = labelEditor.getText();

		Matcher labels = Pattern.compile("\\w*:\\s?[^\\]]*\\]").matcher(temp);

		while (labels.find()) {
			Matcher match = Pattern.compile("\\w*(?=:)").matcher(labels.group(0).trim());

			match.find();

			String header = match.group(0);

			match = Pattern.compile("\\[[^\\]]*\\]").matcher(labels.group(0).trim());

			match.find();

			String body = match.group(0).replaceAll("\\[|\\]", "");

			Label save = new Label(header, body);

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {
						Label temp = null;

						if (saves != null) {
							for (Label l : saves) {
								if (l.getHeader().equals(save.getHeader())) {
									temp = new Label(l.getHeader(), l.getBody());
								}
							}

							if (temp != null) {
								final String t = temp.getHeader();
								saves.removeIf(m -> m.getHeader() == t);
							}
						} else {
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
	private synchronized void saveProject() {
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
	private synchronized void openProject() {
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

	@FXML
	private synchronized void playback() {
		if (loop.isSelected()) {
			MidiBuilder.instance.getPlayEvents().clear();

			try {
				File temp = File.createTempFile("plbk", ".mid", null);
				FileOutputStream fos = new FileOutputStream(temp);
				e.execute(new Runnable() {
					@Override
					public void run() {
						Progression p = assembleProgression();
						MidiBuilder.instance.progChange(instrument);
						p.populatePlayEvents(Integer.parseInt(velocityTextField.getText()));
						try {
							MidiBuilder.instance.writeWithOutputStream(fos);
							Sequence s = MidiSystem.getSequence(temp);

							Track t = s.createTrack();

							ShortMessage sm = new ShortMessage();

							sm.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);

							t.add(new MidiEvent(sm, 0));
							
							midi.open();

							midi.addMetaEventListener(new MetaEventListener() {
								@Override
								public void meta(MetaMessage meta) {
									if (meta.getType() == 0X2F) {
										midi.setTickPosition(0);
										midi.setTempoInBPM(MidiBuilder.instance.tempo);
										midi.start();
									}
								}
							});

							midi.setSequence(s);
							midi.setTempoInBPM(MidiBuilder.instance.tempo);
							midi.start();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (MidiUnavailableException e) {
							e.printStackTrace();
						} catch (InvalidMidiDataException e) {
							e.printStackTrace();
						}
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			this.midi.stop();
		}
	}
	
	@FXML
	private void changeInstrument(ActionEvent e) {
		instrument = this.instrumentBox.getItems().indexOf(this.instrumentBox.getValue());
	}

	/*
	 * HELPER METHODS
	 */

	private synchronized Progression assembleProgression() {
		String chords = chordsTextArea.getText();
		String lRhythm = lRhythmTextArea.getText();
		String rRhythm = rRhythmTextArea.getText();

		chords = handleLabels(chords);
		lRhythm = handleLabels(lRhythm);
		rRhythm = handleLabels(rRhythm);

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
			MidiBuilder.instance.setTimeSignature(Integer.parseInt(num.getText()), Integer.parseInt(den.getText()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (LudicrousTempoException e) {
			e.printStackTrace();
		} catch (MalformedTimeSignatureException e) {
			e.printStackTrace();
		}

		return p;
	}

	private synchronized void macroSave() {
		if (projectDir != null) {
			Project p = new Project(chordsTextArea.getText(), lRhythmTextArea.getText(), rRhythmTextArea.getText());
			try {
				FileOutputStream fs = new FileOutputStream(projectDir);
				ObjectOutputStream os = new ObjectOutputStream(fs);

				os.writeObject(p);

				fs.close();
				os.close();

				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Calendar cal = Calendar.getInstance();

				KeyFrame a = new KeyFrame(Duration.seconds(0), evt -> JZoddRunner.getStage().setTitle(
						String.format("Saved %s at %s", projectDir.getName(), dateFormat.format(cal.getTime()))));
				KeyFrame b = new KeyFrame(Duration.seconds(3), evt -> JZoddRunner.getStage()
						.setTitle(String.format("JZODD [%s] | MIDI Builder", projectDir.getName())));

				Timeline line = new Timeline(a, b);

				line.setCycleCount(0);

				e.execute(new Runnable() {
					@Override
					public void run() {
						line.play();
					}
				});
			} catch (Exception e) {
			}
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

	private synchronized void populateComboBox() {
		Platform.runLater(new Runnable() {
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

						List<String> t = temp.stream().distinct().collect(Collectors.toList());

						labelChooser.setItems(FXCollections.observableArrayList(t));

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
		if (saveDir.isFile()) {
			for (Label l : saves) {
				if (key.equals(l.getHeader())) {
					return l;
				}
			}
		}

		return null;
	}

	private String handleLabels(String s) {
		StringBuilder b = new StringBuilder(s);

		for (Label l : saves) {
			while (b.indexOf(l.getHeader()) != -1) {
				b.replace(b.indexOf(l.getHeader()), b.indexOf(l.getHeader()) + l.getHeader().length(),
						l.getBody().replaceAll("\n|\t", ""));
			}
		}

		return b.toString();
	}

	private String generateDuplicates(String s) {
		StringBuilder sb = new StringBuilder(s);

		dupe(sb, "\\(([^\\)]+)\\)\\s*[xX]\\s*(\\d*)", s);
		dupe(sb, "(.*)\\s*[xX]\\s*(\\d*)", sb.toString());

		return sb.toString().replaceAll(":\\)", ":");
	}

	private synchronized void dupe(StringBuilder sb, String reg, String s) {
		String temp = "";

		Matcher group = Pattern.compile(reg).matcher(s);

		while (group.find()) {
			try {
				String clean = group.group(1);
				toomuch:
				for (int i = 0; i < Integer.parseInt(group.group(2)); i++) {
					if(Integer.parseInt(group.group(2)) < 500) {
						temp += clean + "\n";
					} else {
						break toomuch;
					}
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
		this.tooltips.clear();

		String chords = chordsTextArea.getText();
		String rRhythm = rRhythmTextArea.getText();
		String lRhythm = lRhythmTextArea.getText();

		chords = handleLabels(chords);
		lRhythm = handleLabels(lRhythm);
		rRhythm = handleLabels(rRhythm);

		chords = generateDuplicates(chords);
		rRhythm = generateDuplicates(rRhythm);
		lRhythm = generateDuplicates(lRhythm);

		int numChords = (int) chords.chars().filter(m -> m == '|').count();
		int numChangesR = (int) rRhythm.chars().filter(m -> m == ':').count();
		int numChangesL = (int) lRhythm.chars().filter(m -> m == ':').count();

		chordsLabel.setText(
				String.format("%s (%d)", chordsLabel.getText().replaceAll("\\(\\d*\\)", "").trim(), numChords));

		rRhythmLabel.setText(
				String.format("%s (%d)", rRhythmLabel.getText().replaceAll("\\(\\d*\\)", "").trim(), numChangesR));

		lRhythmLabel.setText(
				String.format("%s (%d)", lRhythmLabel.getText().replaceAll("\\(\\d*\\)", "").trim(), numChangesL));

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

		if(numChords < 500 && numChangesL < 500 && numChangesR < 500) {
			handleCanvasForHand(Color.web("#58A557"), Color.TRANSPARENT, Color.BLACK, lGroove, lSum, lY, var, lCount, p,
					skip, noText);
			handleCanvasForHand(Color.web("#B93D47"), Color.TRANSPARENT, Color.BLACK, rGroove, rSum, rY, var, rCount, p,
					skip, noText);
		}
	}

	private void handleCanvasForHand(Color note, Color rest, Color text, Groove rGroove, int rSum, int rY, int var,
			int rCount, Progression p, int skip, boolean noText) {

		for (TimeEvent i : rGroove.getTimeEvents()) {

			int mod = (i.getTimeEvent() < 0) ? -1 : 1;

			int temp = i.getTimeEvent() * mod;

			Rectangle r;

			if (temp == 0) {
				r = new Rectangle(5 + rSum, 20 + rY, 5, var);
				tooltips.add(new RectanglePairing<Color, Rectangle>(Color.GREY, r));
				rCount++;
			} else {

				if ((rSum + temp) / 400 > 0) {
					rSum = 0;
					rY += skip;
				}

				r = new Rectangle(10 + rSum, 20 + rY, temp, var);

				if (!noText) {
					previewCanvas.getGraphicsContext2D().setFill(Color.BLACK);
					previewCanvas.getGraphicsContext2D().setFont(new Font("SBT-NewCinemaA Std D", 8));
					if(i.getNum() != 0 && i.getArp() == 0) {
						previewCanvas.getGraphicsContext2D().fillText(temp + " + " + i.getNum(), 5 + rSum, 15 + rY);
					} else {
						previewCanvas.getGraphicsContext2D().fillText(temp + "", 5 + rSum, 15 + rY);
					}
				}

				if (rCount == -1) {
					rCount++;
					if (p.getProgressionList().size() > 0 && rCount < p.getProgressionList().size()) {
						String t = p.getProgressionList().get(rCount).getBaseChord();

						if (!noText) {
							Tooltip chord = new Tooltip(t);
							r.getProperties().put("tooltip", chord);
							Tooltip.install(r, chord);
						}
					}
				} else {
					if (p.getProgressionList().size() > 0 && rCount < p.getProgressionList().size()) {
						String t = p.getProgressionList().get(rCount).getBaseChord();

						if (!noText) {
							Tooltip chord = new Tooltip(t);
							r.getProperties().put("tooltip", chord);
							Tooltip.install(r, chord);
						}
					}
				}

				if (mod > 0) {
					tooltips.add(new RectanglePairing<Color, Rectangle>(note, r));
				} else {
					tooltips.add(new RectanglePairing<Color, Rectangle>(rest, r));
				}
			}
			rSum += temp;
		}

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				drawRectangle(previewCanvas, tooltips);
			}
		});
	}

	private void drawRectangle(Canvas canvas, ArrayList<RectanglePairing<Color, Rectangle>> rect) {
		Tooltip tool = new Tooltip();
		Tooltip.install(canvas, tool);
		tool.hide();
		rect.forEach(r -> {
			canvas.getGraphicsContext2D().setFill(r.getC());
			canvas.getGraphicsContext2D().setLineWidth(.5f);
			canvas.getGraphicsContext2D().fillRect(r.getR().getX(), r.getR().getY(), r.getR().getWidth(),
					r.getR().getHeight());
			canvas.getGraphicsContext2D().strokeRect(r.getR().getX(), r.getR().getY(), r.getR().getWidth(),
					r.getR().getHeight());
		});

		canvas.setOnMouseMoved(e -> {
			rect.forEach(r -> {
				if (r.getR().contains(e.getX(), e.getY())) {
					if (r.getR().getProperties().containsKey("tooltip")) {
						tool.setAnchorX(e.getScreenX());
						tool.setAnchorY(e.getScreenY() + 15);
						Tooltip temp = (Tooltip) r.getR().getProperties().get("tooltip");
						if(temp.getText().trim().length() > 0) {
							tool.setText(temp.getText());
						}
					}
				} else {
					tool.hide();
				}
			});
		});

		canvas.setOnMouseExited(e -> {
			tool.hide();
		});
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
	
	private void populateInstruments() {
		try {
			this.synth.open();
			Instrument[] orchestra = this.synth.getAvailableInstruments();
			for(int i = 0; i <= 127; i++) {
				Matcher m = Pattern.compile(": (.*) bank").matcher(orchestra[i].toString());
				String s = "";
				while(m.find()) {
					s += m.group(1);
				}
				this.instrumentBox.getItems().add(String.format("%d: %s", i, s));
			}
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
		
		this.synth.close();
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
		System.setErr(ps);

		populateComboBox();
		populateInstruments();

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
						if (s.match(arg0)) {
							macroSave();
						}
					}
				});
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
