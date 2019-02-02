package driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import exceptions.ColorException;
import exceptions.FrequencyBoundsViolationException;
import exceptions.InvalidInversionException;
import midi.MidiBuilder;

public class Chord {

	/**
	 * @param baseChord Base chord string used by the ChordInterpreter.
	 * @param root      Root note of the chord.
	 * @param quality   Major or minor; major used to differentiate the 7ths,
	 *                  otherwise minor is only true descriptor for augmenting 3rd.
	 * @param color     Additional extended chord tone, (#|b) (5|9|11|13).
	 */
	private String baseChord, root, bass, quality, color, color2;

	/**
	 * @param extension Chord tone accented on top of the base chord.
	 * @param octave    Ya know, octave.
	 * @param inversion Flipping bottom-most note up a certain number of times - 2
	 *                  max for non-extended chords and 3 for extended chords.
	 */
	private int extension, octave, inversion, bassTone;

	/**
	 * @param degrees Number of whole and half steps for the particular chords home
	 *                scale.
	 */
	private int[] degrees;
	private ArrayList<Integer> chordTones;

	String[] noteString = new String[] { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

	public Chord(String[] chordData) {

		this.baseChord = chordData[ChordInterpreter.FULLCHORD];
		this.root = chordData[ChordInterpreter.ROOT];
		this.quality = (chordData[ChordInterpreter.QUALITY] == null) ? "" : chordData[ChordInterpreter.QUALITY];
		this.extension = (chordData[ChordInterpreter.EXTENSION] != null)
				? Integer.parseInt(chordData[ChordInterpreter.EXTENSION])
				: 0;
		this.color = (chordData[ChordInterpreter.COLOR] == null) ? "" : chordData[ChordInterpreter.COLOR];
		this.color2 = (chordData[ChordInterpreter.COLOR2] == null) ? "" : chordData[ChordInterpreter.COLOR2];
		this.bass = (chordData[ChordInterpreter.BASS] == null) ? "" : chordData[ChordInterpreter.BASS];
		this.octave = Integer.parseInt(chordData[ChordInterpreter.OCTAVE]);
		this.inversion = (int) chordData[ChordInterpreter.INVERSION].chars().filter(ch -> ch == '^').count();

		this.chordTones = new ArrayList<Integer>();

		try {
			if (this.extension == 7 || this.extension == 6) {
				if (this.inversion > 3) {
					throw new InvalidInversionException();
				}
			} else if (this.extension == 9) {
				if (this.inversion > 4) {
					throw new InvalidInversionException();
				}
			} else if (this.extension == 11) {
				if (this.inversion > 5) {
					throw new InvalidInversionException();
				}
			} else if (this.extension == 13) {
				if (this.inversion > 6) {
					throw new InvalidInversionException();
				}
			} else {
				if (this.inversion > 2) {
					throw new InvalidInversionException();
				}
			}

			if (getNoteHashMap().get(this.root)[this.octave] > getNoteHashMap().get("C")[8]
					|| getNoteHashMap().get(this.root)[this.octave] < getNoteHashMap().get("A")[0]) {
				throw new FrequencyBoundsViolationException();
			}
		} catch (InvalidInversionException | FrequencyBoundsViolationException e) {
			e.printStackTrace();
		}

		assembleTones();
	}

	private void assembleTones() {
		processRoot();
		processQuality();
		if (this.extension > 0) {
			processExtension();
		}
		if (this.color.trim().length() > 0) {
			try {
				processColor(this.color);
				if (this.color2.trim().length() > 0) {
					processColor(this.color2);
				}
			} catch (ColorException e) {
				e.printStackTrace();
			}
		}
		processInversion();
		processBass();
	}

	private void processRoot() {
		int midiNumForRoot = getChordInterpreter()
				.getMidiNumber(getChordInterpreter().getNoteHashMap().get(this.root)[this.octave]);
		this.chordTones.add(new Integer(midiNumForRoot));
	}

	private void processQuality() {
		int root = this.chordTones.get(0);
		switch (this.quality) {
		case "maj":
			// major 3rd
			this.chordTones.add(root + 4);
			// perfect 5th
			this.chordTones.add(root + 7);
			break;
		case "":
			// major 3rd
			this.chordTones.add(root + 4);
			// perfect 5th
			this.chordTones.add(root + 7);
			break;
		case "min":
			// minor third
			this.chordTones.add(root + 3);
			// perfect 5th
			this.chordTones.add(root + 7);
			break;
		case "aug":
			// major 3rd
			this.chordTones.add(root + 4);
			// augmented 5th
			this.chordTones.add(root + 8);
			break;
		case "dim":
			// minor third
			this.chordTones.add(root + 3);
			// diminished 5th
			this.chordTones.add(root + 6);
			break;
		case "sus2":
			// 2nd degree
			this.chordTones.add(root + 2);
			// perfect 5th
			this.chordTones.add(root + 7);
			break;
		case "sus4":
			// perfect 4th
			this.chordTones.add(root + 5);
			// perfect 5th
			this.chordTones.add(root + 7);
			break;
		}
	}

	// (6|7|9|11|13)?([#b]?(?:5|9|11|13)?)?
	private void processExtension() {
		int root = this.chordTones.get(0);
		switch (this.extension) {
		case 6:
			this.chordTones.add(root + 9);
			break;
		case 7:
			if (this.quality.indexOf("maj") == -1) {
				this.chordTones.add(root + 10);
			} else {
				this.chordTones.add(root + 11);
			}
			break;
		case 9:
			this.chordTones.add(root + 10);
			this.chordTones.add(root + 14);
			break;
		case 11:
			this.chordTones.add(root + 10);
			this.chordTones.add(root + 14);
			this.chordTones.add(root + 17);
			break;
		case 13:
			this.chordTones.add(root + 10);
			this.chordTones.add(root + 14);
			this.chordTones.add(root + 17);
			this.chordTones.add(root + 21);
			break;
		}
	}

	private void processColor(String color) throws ColorException {
		int modifier = 0;
		if (color.charAt(0) == '#') {
			modifier = 1;
		} else if (color.charAt(0) == 'b') {
			modifier = -1;
		}

		switch (Integer.parseInt(color.substring(1))) {
		case 5:
			this.chordTones.set(2, this.chordTones.get(2) + modifier);
			break;
		case 9:
			if (this.extension == 9) {
				throw new ColorException();
			} else if (this.extension < 9) {
				this.chordTones.add(this.chordTones.get(0) + 14 + modifier);
			} else if (this.extension > 9 && this.extension <= 13) {
				this.chordTones.set(4, this.chordTones.get(4) + modifier);
			}
			break;
		case 11:
			if (this.extension == 11) {
				throw new ColorException();
			} else if (this.extension < 11) {
				this.chordTones.add(this.chordTones.get(0) + 17 + modifier);
			} else if (this.extension == 13) {
				this.chordTones.set(5, this.chordTones.get(5) + modifier);
			}
			break;
		case 13:
			if (this.extension == 13) {
				throw new ColorException();
			} else if (this.extension == 7 || this.extension == 9 || this.extension == 11) {
				this.chordTones.add(this.chordTones.get(0) + 21 + modifier);
			}
			break;
		}
	}

	private void processInversion() {
		switch (this.extension) {
		case 9:
			this.chordTones.set(3, this.chordTones.get(3) - 12);
			this.chordTones.set(4, this.chordTones.get(4) - 12);
			for (int i = 0; i < this.inversion; i++) {
				int index = this.chordTones.indexOf(Collections.min(this.chordTones));
				this.chordTones.set(index, this.chordTones.get(index) + 12);
			}
			break;
		case 11:
			this.chordTones.remove(2);
			this.chordTones.set(3, this.chordTones.get(3) - 12);
			this.chordTones.set(4, this.chordTones.get(4) - 12);
			for (int i = 0; i < this.inversion; i++) {
				int index = this.chordTones.indexOf(Collections.min(this.chordTones));
				this.chordTones.set(index, this.chordTones.get(index) + 12);
			}
			break;
		case 13:
			this.chordTones.remove(5);
			this.chordTones.set(3, this.chordTones.get(3) - 12);
			this.chordTones.set(4, this.chordTones.get(4) - 12);
			this.chordTones.set(5, this.chordTones.get(4) - 12);
			for (int i = 0; i < this.inversion; i++) {
				int index = this.chordTones.indexOf(Collections.min(this.chordTones));
				this.chordTones.set(index, this.chordTones.get(index) + 12);
			}
			break;
		default:
			for (int i = 0; i < this.inversion; i++) {
				int index = this.chordTones.indexOf(Collections.min(this.chordTones));
				this.chordTones.set(index, this.chordTones.get(index) + 12);
			}
			break;
		}
		Collections.sort(this.chordTones);
	}

	private void processBass() {
		double freq;

		if (this.bass.trim().length() == 0) {
			freq = getChordInterpreter().getNoteHashMap().get(this.root)[this.octave];
			this.bass = this.root;
		} else {
			freq = getChordInterpreter().getNoteHashMap().get(this.bass)[this.octave];
		}

		int midiNum = getChordInterpreter().getMidiNumber(freq);
		this.bassTone = (midiNum >= Collections.min(this.chordTones)) ? midiNum - 12 : midiNum - 12;
	}

	public void setBassTone(int tone) {
		this.bassTone = tone;
	}

	private HashMap<String, double[]> getNoteHashMap() {
		return MidiBuilder.instance.getChordInterpreter().getNoteHashMap();
	}

	private ChordInterpreter getChordInterpreter() {
		return MidiBuilder.instance.getChordInterpreter();
	}

	public ArrayList<Integer> getChordTones() {
		return this.chordTones;
	}

	public String getBass() {
		return this.bass;
	}

	public int getBassTone() {
		return this.bassTone;
	}

	public String getRoot() {
		return this.root;
	}

	public int getOctave() {
		return this.octave;
	}

	public String toString() {
		String s;
		if (this.bass != "") {
			s = String.format("%s | %s%s%d%s/%s:O%dI%d...", this.baseChord, this.root, this.quality, this.extension,
					this.color, this.bass, this.octave, this.inversion);
		} else {
			s = String.format("%s | %s%s%d%s:O%dI%d...", this.baseChord, this.root, this.quality, this.extension,
					this.color, this.octave, this.inversion);
		}
		return s;
	}
}
