package driver;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import midi.MidiBuilder;

public class ChordInterpreter {

	public static final int FULLCHORD = 0, ROOT = 1, QUALITY = 2, EXTENSION = 3, COLOR = 4, COLOR2 = 5,  BASS = 6, OCTAVE = 7, INVERSION = 8;
	
	private HashMap<String, double[]> noteValues;
	
	private HashMap<String, int[]> modes;

	public ChordInterpreter() {
		initializeNoteHashMap();
		initializeModes();
	}

	public String[] organizeChord(String chord) {

		String[] matchArray = new String[] {"", "", "", "", "", "", "", "", ""};

		Matcher m = Pattern.compile(
				"([A-Ga-g][#b]?)(min|maj|aug|dim|sus[24])?(6|7|9|11|13)?([#b]?(?:5|9|11|13)?)?([#b]?(?:5|9|11|13)?)?[\\/]?([A-Ga-g][#b]?)?"
				+ ":?([0-8])?(\\^*)?")
				.matcher(chord);

		while (m.find()) {
			for (int i = 0; i < matchArray.length; i++) {
				matchArray[i] = m.group(i);
			}
		}

		return matchArray;
	}

	public int getMidiNumber(double freq) {
		return (int) Math.round((12 * MidiBuilder.log(freq / 440, 2) + 69));
	}

	private void initializeNoteHashMap() {
		this.noteValues = new HashMap<String, double[]>();
		noteValues.put("C", new double[] { 16.35, 32.70, 65.41, 130.81, 261.63, 523.25, 1046.50, 2093.00, 4186.01 });
		noteValues.put("C#", new double[] { 17.32, 34.65, 69.30, 138.59, 277.18, 554.37, 1108.73, 2217.46, 4434.92 });
		noteValues.put("Db", new double[] { 17.32, 34.65, 69.30, 138.59, 277.18, 554.37, 1108.73, 2217.46, 4434.92 });
		noteValues.put("D", new double[] { 18.35, 36.71, 73.42, 146.83, 293.66, 587.33, 1174.66, 2349.32, 4698.64 });
		noteValues.put("D#", new double[] { 19.45, 38.89, 77.78, 155.56, 311.13, 622.25, 1244.51, 2489.02, 4978.03 });
		noteValues.put("Eb", new double[] { 19.45, 38.89, 77.78, 155.56, 311.13, 622.25, 1244.51, 2489.02, 4978.03 });
		noteValues.put("E", new double[] { 20.60, 41.20, 82.41, 164.81, 329.63, 659.26, 1318.51, 2637.02 });
		noteValues.put("F", new double[] { 21.83, 43.65, 87.31, 174.61, 349.23, 698.46, 1396.91, 2793.83 });
		noteValues.put("F#", new double[] { 23.12, 46.25, 92.50, 185.00, 369.99, 739.99, 1479.98, 2959.96 });
		noteValues.put("Gb", new double[] { 23.12, 46.25, 92.50, 185.00, 369.99, 739.99, 1479.98, 2959.96 });
		noteValues.put("G", new double[] { 24.50, 49.00, 98.00, 196.00, 392.00, 783.99, 1567.98, 3135.96 });
		noteValues.put("G#", new double[] { 25.96, 51.91, 103.83, 207.65, 415.30, 830.61, 1661.22, 3322.44 });
		noteValues.put("Ab", new double[] { 25.96, 51.91, 103.83, 207.65, 415.30, 830.61, 1661.22, 3322.44 });
		// 27.5 == A0
		noteValues.put("A", new double[] { 27.50, 55.00, 110.00, 220.00, 440.00, 880.00, 1760.00, 3520.00 });
		noteValues.put("A#", new double[] { 29.14, 58.27, 116.54, 233.08, 466.16, 932.33, 1864.66, 3729.31 });
		noteValues.put("Bb", new double[] { 29.14, 58.27, 116.54, 233.08, 466.16, 932.33, 1864.66, 3729.31 });
		noteValues.put("B", new double[] { 30.87, 61.74, 123.47, 246.94, 493.88, 987.77, 1975.53, 3951.07 });
	}
	
	private void initializeModes() {
		this.modes = new HashMap<String, int[]>();
		modes.put("Ionian", new int[] {0,2,2,1,2,2,2,1});
		modes.put("Lydian", new int[] {0,2,2,2,1,2,2,1});
		modes.put("Mixolydian", new int[] {0,2,2,1,2,2,1,2});
		modes.put("Dorian", new int[] {0,2,1,2,2,2,1,2});
		modes.put("Aeolian", new int[] {0,2,1,2,2,1,2,2});
		modes.put("Harmonic Minor", new int[] {0,2,1,2,2,1,3,1});
		modes.put("Aeolian", new int[] {0,2,1,2,2,1,2,2});
		modes.put("Phrygian", new int[] {0,1,2,2,2,1,2,2});
		modes.put("Locrian", new int[] {0,1,2,2,1,2,2,2});
	}

	public HashMap<String, double[]> getNoteHashMap() {
		return this.noteValues;
	}
	
	public HashMap<String, int[]> getModes() {
		return this.modes;
	}
}
