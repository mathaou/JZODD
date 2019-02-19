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

/**
 * 
 * @author Matt Farstad
 * @version 1.0
 *
 */
public class Progression {

	/**
	 * @param match the regex matcher used to parse the
	 */
	private Matcher match;

	/**
	 * @param progressionList Holds the chords defined in editor.
	 */
	private ArrayList<Chord> progressionList;

	/**
	 * @param grooveList Array of size 2 that holds the grooves.
	 */
	private Groove[] grooveList;

	/**
	 * @param eventQueue A FIFO data structure that holds the list of events as they happen.
	 */
	private Queue<int[]> eventQueue;

	/**
	 * Constructor.
	 * @param progression String from text area.
	 */
	public Progression(String progression) {
		this.progressionList = new ArrayList<Chord>();
		this.grooveList = new Groove[2];
		this.eventQueue = new LinkedList<>();

		this.match = Pattern.compile(".+?(?= ?\\| ?\\|?)").matcher(progression);
		while (this.match.find()) {
			String temp = this.match.group().replaceAll("\\s?\\|?\\s\\|?", "").trim();
			if (temp.length() > 0) {
				String[] chord = MidiBuilder.instance.getChordInterpreter().organizeChord(temp);
				
				//octave set to 4 if not defined
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

	/**
	 * Generates MIDI file.
	 * @param vel Velocity as an integer.
	 */
	public synchronized void generateMidi(int vel) {
		//makes sure that groove exists on a hand
		try {
			if (this.grooveList[0] == null && this.grooveList[1] == null) {
				throw new NoFunkException();
			}
		} catch (NoFunkException e) {
			e.printStackTrace();
		}

		//creates all events as int arrays
		processGrooveChannels(this.grooveList[0], this.grooveList[1], vel);

		//writes to vector
		for (int[] i : this.eventQueue) {
			MidiBuilder.instance.getPlayEvents().add(i);
		}
		
		//file writing happens on thread so app doesnt slow down
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
	
	public synchronized void populatePlayEvents(int vel) {
		try {
			if (this.grooveList[0] == null && this.grooveList[1] == null) {
				throw new NoFunkException();
			}
		} catch (NoFunkException e) {
			e.printStackTrace();
		}

		//creates all events as int arrays
		processGrooveChannels(this.grooveList[0], this.grooveList[1], vel);

		//writes to vector
		for (int[] i : this.eventQueue) {
			MidiBuilder.instance.getPlayEvents().add(i);
		}
	}

	/**
	 * Add groove channel if available and makes sure that number of chord changes == number of chords.
	 * @param g Groove to add.
	 */
	public synchronized void addGrooveChannel(Groove g) {
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

	/**
	 * Processes groove channels. Very complex method but it has to account for a lot and it works so I'd call it a good job.
	 * @param g1 Groove for left hand.
	 * @param g2 Groove for right hand.
	 * @param vel Velocity as int.
	 */
	public synchronized void processGrooveChannels(Groove g1, Groove g2, int vel) {
		//define variables needed
		int lCount = 0, rCount = 0, lpd = 0, lPeek = 0, rPeek = 0, lSum = 0, rSum = 0, t = 0, lNum = 0, rNum = 0, rArp = 0;

		//needed to keep track of rests
		boolean lRest = true, rRest = true;

		//while either queue has something in it (FIFO data structure is very helpful for this).
		while (!g1.getTimeEvents().isEmpty() && !g2.getTimeEvents().isEmpty()) {

			//peek both stacks
			lPeek = g1.getTimeEvents().peek().getTimeEvent();
			rPeek = g2.getTimeEvents().peek().getTimeEvent();
			
			//as well as any note modifications
			lNum = g1.getTimeEvents().peek().getNum();
			rNum = g2.getTimeEvents().peek().getNum();
			
			rArp = g2.getTimeEvents().peek().getArp();

			//get the modifier in case of rest
			int lModifier = (lPeek < 0) ? -1 : 1;
			int rModifier = (rPeek < 0) ? -1 : 1;

			//fix peek
			lPeek *= lModifier;
			rPeek *= rModifier;

			//get currently defined chord based on either hands count
			Chord lC = this.progressionList.get(lCount);
			Chord rC = this.progressionList.get(rCount);

			//right away noteOn
			if (t == 0) {
				//rests for either hand if modifier -1
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

				//left hand
				if (lModifier == 1) {
					//bass adds note modifier directly to tone
					if (g1.isBass()) {
						int[] a = noteOn(0, lC.getBassTone() + lNum, vel);
						this.eventQueue.add(a);
					} else {
						//else note modifier denotes chord tone to play
						if(lNum > 0 && lNum <= lC.getChordTones().size()) {
							int[] a = noteOn(0, lC.getChordTones().get(lNum-1) - 12, vel);
							this.eventQueue.add(a);
						} else {
							//voice chord
							for (Integer i : lC.getChordTones()) {
								int[] a = noteOn(0, i - 12, vel);
								this.eventQueue.add(a);
							}
						}
					}
				}

				//same process for right hand
				if (rModifier == 1) {
					//right hand cant be bass
					if(rArp == -1) {
						int size = rC.getChordTones().size(), help = 0, num = 0, mod = 1, inner = 0, outer = 0;
						if(size % 2 == 0) {
							inner = size - 1;
							outer = size;
						} else {
							inner = (size - 1) * 2;
							outer = size;
						}
						
						int[][] ret = new int[inner][outer];
						
						while(help != (outer * (inner))) {
							ret[help/size][help%size] = num;
							num += mod;
							if(num == size - 1 || num == 0) mod *= -1;
							help++;
						}
						int[] a = noteOn(0, rC.getChordTones().get(ret[rNum/size][rNum%size]), vel);
						this.eventQueue.add(a);
					} else if(rNum > 0 && rNum <= rC.getChordTones().size()) {
						int[] a = noteOn(0, rC.getChordTones().get(rNum-1), vel);
						this.eventQueue.add(a);
					} else {
						//voice chord
						for (Integer i : rC.getChordTones()) {
							int[] a = noteOn(0, i, vel);
							this.eventQueue.add(a);
						}
					}
				}
			}

			//take care of left hand
			if ((t - lSum) == lPeek) {
				//if rest and note on event exists for rest
				if (lModifier == -1 && !lRest) {
					int[] a = noteOff(t - lpd, -1);
					this.eventQueue.add(a);
					//keep track of last played delta
					lpd = t;
					//add to lSum
					lSum += g1.getTimeEvents().poll().getTimeEvent();
					//reset rest boolean
					lRest = true;
				} else {
					//if bass take care of that with note modifier
					if (g1.isBass()) {
						int[] a = noteOff(t - lpd, lC.getBassTone() + lNum);
						this.eventQueue.add(a);
					} else {
						//else voice chord tone
						//temp needed for note off
						int temp = t - lpd;
						if(lNum > 0 && lNum <= lC.getChordTones().size()) {
							int[] a = noteOff(temp, lC.getChordTones().get(lNum-1) - 12);
							this.eventQueue.add(a);
						} else {
							//voice chord
							for (Integer i : lC.getChordTones()) {
								int[] a = noteOff(temp, i - 12);
								this.eventQueue.add(a);
								//successive notes key off of lpd, which is the current note so 0
								temp = 0;
							}
						}
					}

					//set lpd
					lpd = t;
					//add to lSum
					lSum += g1.getTimeEvents().poll().getTimeEvent();
				}

				//temp to check for chord change or end of track
				int temp = g1.getTimeEvents().peek().getTimeEvent();
				//get next note modifier
				lNum = g1.getTimeEvents().peek().getNum();

				//if temp == 0
				if (temp == 0) {
					//poll to remove event
					g1.getTimeEvents().poll();

					//if next peek is null end of queue so continue
					if (g1.getTimeEvents().peek() == null)
						continue;

					//increment chord count
					lCount++;
					//reset chord
					lC = this.progressionList.get(lCount);

					//set temp to be next time event
					temp = g1.getTimeEvents().peek().getTimeEvent();
					//move note modifier to next
					lNum = g1.getTimeEvents().peek().getNum();
				}

				//get lModifier to check for rest
				lModifier = (temp < 0) ? -1 : 1;

				//if rest available
				if (lModifier < 0 && lRest) {
					int[] a = noteOn(0, -1, 0);
					this.eventQueue.add(a);
					//set noteOn event to true
					lRest = false;
				} else {
					//handle bass
					if (g1.isBass()) {
						int[] a = noteOn(0, lC.getBassTone() + lNum, vel);
						this.eventQueue.add(a);
					} else {
						//voice specific chord tone
						if(lNum > 0 && lNum <= lC.getChordTones().size()) {
							int[] a = noteOn(0, lC.getChordTones().get(lNum-1) - 12, vel);
							this.eventQueue.add(a);
						} else {
							//voice chord
							for (Integer i : lC.getChordTones()) {
								int[] a = noteOn(0, i - 12, vel);
								this.eventQueue.add(a);
							}
						}
					}
				}
			}

			//repeat for right hand, but no need to handle bass
			if ((t - rSum) == rPeek) {
				//if rest and note on detected
				if (rModifier == -1 && !rRest) {
					int[] a = noteOff(t - lpd, -1);
					this.eventQueue.add(a);
					//set last played delta
					lpd = t;
					//add next to sum
					rSum += g2.getTimeEvents().poll().getTimeEvent();
					//reset rest
					rRest = true;
				} else {
					//temp needed for multiple notes
					int temp = t - lpd;
					//note modifier denotes chord tone to play
					if(rArp == -1) {
						int size = rC.getChordTones().size(), help = 0, num = 0, mod = 1, inner = 0, outer = 0;
						if(size % 2 == 0) {
							inner = size - 1;
							outer = size;
						} else {
							inner = (size - 1) * 2;
							outer = size;
						}
						
						int[][] ret = new int[inner][outer];
						
						while(help != (outer * (inner))) {
							ret[help/size][help%size] = num;
							num += mod;
							if(num == size - 1 || num == 0) mod *= -1;
							help++;
						}
						int[] a = noteOff(temp, rC.getChordTones().get(ret[rNum/size][rNum%size]));
						this.eventQueue.add(a);
					} else if(rNum > 0 && rNum <= rC.getChordTones().size()) {
						int[] a = noteOff(temp, rC.getChordTones().get(rNum-1));
						this.eventQueue.add(a);
					} else {
						//voice chord
						for (Integer i : rC.getChordTones()) {
							int[] a = noteOff(temp, i);
							this.eventQueue.add(a);
							//notes key off of last played note so notes on current value must be 0
							temp = 0;
						}
					}

					//set lpd
					lpd = t;
					//add next to sum
					rSum += g2.getTimeEvents().poll().getTimeEvent();
				}

				//get next time event to check for chord change or end of track
				int temp = g2.getTimeEvents().peek().getTimeEvent();
				//get next note modifier
				rNum = g2.getTimeEvents().peek().getNum();
				
				rArp = g2.getTimeEvents().peek().getArp();

				if (temp == 0) {
					//move on to next
					g2.getTimeEvents().poll();

					//if end of track continue
					if (g2.getTimeEvents().peek() == null)
						continue;

					//increment count for hand
					rCount++;
					//reset chord
					rC = this.progressionList.get(rCount);

					//get next temp and note modifier
					temp = g2.getTimeEvents().peek().getTimeEvent();
					rNum = g2.getTimeEvents().peek().getNum();
					rArp = g2.getTimeEvents().peek().getArp();
				}

				//get rest
				rModifier = (temp < 0) ? -1 : 1;
				
				//if rest available
				if (rModifier < 0 && rRest) {
					int[] a = noteOn(0, -1, 0);
					this.eventQueue.add(a);
					//set noteOn event to true
					rRest = false;
				} else {
					//note modifier denotes chord tone to voice
					if(rArp == -1) {
						int size = rC.getChordTones().size(), help = 0, num = 0, mod = 1, inner = 0, outer = 0;
						if(size % 2 == 0) {
							inner = size - 1;
							outer = size;
						} else {
							inner = (size - 1) * 2;
							outer = size;
						}
						
						int[][] ret = new int[inner][outer];
						
						while(help != (outer * (inner))) {
							ret[help/size][help%size] = num;
							num += mod;
							if(num == size - 1 || num == 0) mod *= -1;
							help++;
						}
						int[] a = noteOn(0, rC.getChordTones().get(ret[rNum/size][rNum%size]), vel);
						this.eventQueue.add(a);
					} else if(rNum > 0 && rNum <= rC.getChordTones().size()) {
						int[] a = noteOn(0, rC.getChordTones().get(rNum-1), vel);
						this.eventQueue.add(a);
					} else {
						//voice chord
						for (Integer i : rC.getChordTones()) {
							int[] a = noteOn(0, i, vel);
							this.eventQueue.add(a);
						}
					}
				}
			}

			//increment by 16th note
			t += Groove.SIXTEENTH;
		}
	}

	/**
	 * creates a note on event.
	 * @return int[] noteOn event.
	 */
	private static int[] noteOn(int delta, int note, int velocity) {
		int[] data = new int[4];
		data[0] = delta;
		data[1] = 0x90;
		data[2] = note;
		data[3] = velocity;
		return data;
	}

	/**
	 * creates a note off event.
	 * @return int[] noteOff event.
	 */
	private static int[] noteOff(int delta, int note) {
		int[] data = new int[4];
		data[0] = delta;
		data[1] = 0x80;
		data[2] = note;
		data[3] = 0;
		return data;
	}

	public ArrayList<Chord> getProgressionList(){
		return this.progressionList;
	}
}
