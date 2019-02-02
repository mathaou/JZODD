package midi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import driver.Chord;
import driver.ChordInterpreter;
import driver.Groove;
import exceptions.NoFunkException;
import exceptions.TooMuchBassException;
import exceptions.TooMuchFunkException;
import exceptions.UndefinedChordException;
import exceptions.UndefinedRhtyhmException;
import javafx.application.Platform;

public class Progression {

	private Matcher match;

	private ArrayList<Chord> progressionList;

	private Groove[] grooveList;

	private Queue<int[]> eventQueue;

	public Progression(String progression) {
		this.progressionList = new ArrayList<Chord>();
		this.grooveList = new Groove[2];
		this.eventQueue = new LinkedList<>();

		this.match = Pattern.compile(".+?(?= ?\\| ?\\|?)").matcher(progression);
		while (this.match.find()) {
			String temp = this.match.group().replaceAll("\\s?\\|?\\s\\|?", "").trim();
			if (temp.length() > 0) {
				String[] chord = MidiBuilder.instance.getChordInterpreter().organizeChord(temp);
				if (chord[ChordInterpreter.OCTAVE] == null) {
					chord[ChordInterpreter.OCTAVE] = "4";
				}
				chord[ChordInterpreter.ROOT] = chord[ChordInterpreter.ROOT].substring(0, 1).toUpperCase()
						+ chord[ChordInterpreter.ROOT].substring(1);
				if (chord[ChordInterpreter.BASS] != null) {
					chord[ChordInterpreter.BASS] = chord[ChordInterpreter.BASS].substring(0, 1).toUpperCase()
							+ chord[ChordInterpreter.BASS].substring(1);
				}
				int size = this.progressionList.size();
				this.progressionList.add(new Chord(chord));

				// this is where the bass note correction happens
				if (size > 1) {
					if (this.progressionList.get(size - 1).getBassTone() < this.progressionList.get(size - 2)
							.getBassTone()
							&& this.progressionList.get(size - 1).getBass()
									.equals(this.progressionList.get(size - 2).getBass())) {
						this.progressionList.get(size - 1)
								.setBassTone(this.progressionList.get(size - 1).getBassTone() + 12);
					}
				}
			}
		}
	}

	public void generateMidi(int vel) {
		try {
			if (this.grooveList[0] == null && this.grooveList[1] == null) {
				throw new NoFunkException();
			}
		} catch (NoFunkException e) {
			e.printStackTrace();
		}

		processGrooveChannels(this.grooveList[0], this.grooveList[1], vel);

		for (int[] i : this.eventQueue) {
			MidiBuilder.instance.getPlayEvents().add(i);
		}
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					MidiBuilder.instance.writeToFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		});
	}

	public void addGrooveChannel(Groove g) {
		int i = 0;
		for (String s : g.matches) {
			i += (int) s.chars().filter(p -> p == ':').count();
		}
		try {
			if (i > progressionList.size()) {
				throw new UndefinedChordException();
			}
			if (i < progressionList.size()) {
				throw new UndefinedRhtyhmException();
			} else {
				if (this.grooveList[0] == null) {
					this.grooveList[0] = g;
				} else if (this.grooveList[0] != null) {
					this.grooveList[1] = g;
				} else {
					throw new TooMuchFunkException();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// extract notes out to be processed later (thats really hard)
	public void processGrooveChannels(Groove g1, Groove g2, int vel) {
		int lCount = 0, rCount = 0, lpd = 0, lPeek = 0, rPeek = 0, lSum = 0, rSum = 0, t = 0;

		boolean lRest = true, rRest = true;

		while (!g1.getTimeEvents().isEmpty() && !g2.getTimeEvents().isEmpty()) {

			lPeek = g1.getTimeEvents().peek();
			rPeek = g2.getTimeEvents().peek();

			int lModifier = (lPeek < 0) ? -1 : 1;
			int rModifier = (rPeek < 0) ? -1 : 1;

			lPeek *= lModifier;
			rPeek *= rModifier;

			Chord lC = this.progressionList.get(lCount);
			Chord rC = this.progressionList.get(rCount);

			if (t == 0) {
				if (lModifier == -1) {
					int[] a = noteOn(0, -1, 0);
					this.eventQueue.add(a);
					lRest = false;
				}

				if (rModifier == -1) {
					int[] a = noteOn(0, -1, 0);
					this.eventQueue.add(a);
					rRest = false;
				}

				if (lModifier == 1) {
					if (g1.isBass()) {
						int[] a = noteOn(0, lC.getBassTone(), vel);
						this.eventQueue.add(a);
					} else {
						for (Integer i : lC.getChordTones()) {
							int[] a = noteOn(0, i, vel);
							this.eventQueue.add(a);
						}
					}
				}

				if (rModifier == 1) {
					if (g2.isBass()) {
						int[] a = noteOn(0, rC.getBassTone(), vel);
						this.eventQueue.add(a);
					} else {
						for (Integer i : rC.getChordTones()) {
							int[] a = noteOn(0, i, vel);
							this.eventQueue.add(a);
						}
					}
				}
			}

			if ((t - lSum) == lPeek) {
				if (lModifier == -1 && !lRest) {
					int[] a = noteOff(t - lpd, -1);
					this.eventQueue.add(a);
					lpd = t;
					g1.getTimeEvents().poll();
					lSum += lPeek;
					lRest = true;
				} else {
					if (g1.isBass()) {
						int[] a = noteOff(t - lpd, lC.getBassTone());
						this.eventQueue.add(a);
					} else {
						int temp = t - lpd;
						for (Integer i : lC.getChordTones()) {
							int[] a = noteOff(temp, i);
							this.eventQueue.add(a);
							temp = 0;
						}
					}

					lpd = t;
					lSum += g1.getTimeEvents().poll();
				}

				int temp = g1.getTimeEvents().peek();

				if (temp == 0) {
					g1.getTimeEvents().poll();

					if (g1.getTimeEvents().peek() == null)
						continue;

					lCount++;
					lC = this.progressionList.get(lCount);

					temp = g1.getTimeEvents().peek();
				}

				lModifier = (temp < 0) ? -1 : 1;

				lPeek *= lModifier;

				if (lModifier < 0 && lRest) {
					int[] a = noteOn(0, -1, 0);
					this.eventQueue.add(a);
					lRest = false;
				} else {
					if (g1.isBass()) {
						int[] a = noteOn(0, lC.getBassTone(), vel);
						this.eventQueue.add(a);
					} else {
						for (Integer i : lC.getChordTones()) {
							int[] a = noteOn(0, i, vel);
							this.eventQueue.add(a);
						}
					}
				}
			}

			if ((t - rSum) == rPeek) {
				if (rModifier == -1 && !rRest) {
					int[] a = noteOff(t - lpd, -1);
					this.eventQueue.add(a);

					lpd = t;
					g2.getTimeEvents().poll();
					rSum += rPeek;
					rRest = true;
				} else {
					if (g2.isBass()) {
						int[] a = noteOff(t - lpd, rC.getBassTone());
						this.eventQueue.add(a);
					} else {
						int temp = t - lpd;
						for (Integer i : rC.getChordTones()) {
							int[] a = noteOff(temp, i);
							this.eventQueue.add(a);
							temp = 0;
						}
					}

					lpd = t;
					rSum += g2.getTimeEvents().poll();
				}

				int temp = g2.getTimeEvents().peek();

				if (temp == 0) {
					g2.getTimeEvents().poll();

					if (g2.getTimeEvents().peek() == null)
						continue;

					rCount++;
					rC = this.progressionList.get(rCount);

					temp = g2.getTimeEvents().peek();
				}

				rModifier = (temp < 0) ? -1 : 1;

				rPeek *= rModifier;

				if (rModifier < 0 && rRest) {
					int[] a = noteOn(0, -1, 0);
					this.eventQueue.add(a);
					rRest = false;
				} else {
					if (g2.isBass()) {
						int[] a = noteOn(0, rC.getBassTone(), vel);
						this.eventQueue.add(a);
					} else {
						for (Integer i : rC.getChordTones()) {
							int[] a = noteOn(0, i, vel);
							this.eventQueue.add(a);
						}
					}
				}
			}

			t += Groove.SIXTEENTH;
		}
	}

	public static int[] noteOn(int delta, int note, int velocity) {
		int[] data = new int[4];
		data[0] = delta;
		data[1] = 0x90;
		data[2] = note;
		data[3] = velocity;
		return data;
	}

	public static int[] noteOff(int delta, int note) {
		int[] data = new int[4];
		data[0] = delta;
		data[1] = 0x80;
		data[2] = note;
		data[3] = 0;
		return data;
	}

}
