package driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import exceptions.IntervalGroupingException;

/**
 * 
 * @author Matt Farstad
 * @version 1.0
 */
public class Groove {

	/**
	 * @param timeEvents Queue of time events used in generating midi file.
	 */
	public Queue<TimeEvent> timeEvents;
	
	/**
	 * @param matches list of matches from regex.
	 */
	public ArrayList<String> matches;
	
	/**
	 * @param bass detects if bass (lHand only)
	 */
	public Boolean bass;

	/**
	 * @param SIXTEENTH Integer value for a 16th note.
	 * @param EIGHTH Integer value for an 8th note.
	 * @param QUARTER Integer value for a quarter note.
	 * @param HALF Integer value for a half note.
	 * @param WHOLE Integer value for a whole note.
	 */
	public static final int SIXTEENTH = 4, EIGHTH = 8, QUARTER = 16, HALF = 32, WHOLE = 64;

	/**
	 * Constructor.
	 * @param bass BASS.
	 * @param groove Groove from text area.
	 */
	public Groove(Boolean bass, String groove) {
		this.bass = bass;
		this.matches = new ArrayList<String>();
		this.timeEvents = new LinkedList<>();

		Matcher match = Pattern.compile("[seqhwSEQHW](\\+[seqwhSEQHW])*(\\(-?\\d*\\)|\\{\\d*\\})?[:]?").matcher(groove);
		while (match.find()) {
			this.matches.add(match.group());
		}
		convertTime();
	}

	/**
	 * Converts matches into integer representations of duration.
	 */
	private void convertTime() {
		for (String s : this.matches) {

			//uninitialized TimeEvent object
			TimeEvent event;
			
			//note modifications
			Matcher number = Pattern.compile("\\(-?\\d+\\)").matcher(s);
			
			int num = 0;
			
			while(number.find()) {
				num = Integer.parseInt(number.group(0).replaceAll("\\(|\\)", ""));
			}
			
			//arpeggios soon!
			Matcher arpegg = Pattern.compile("\\{\\d*\\}").matcher(s);
			
			int arp = -1;
			
			while(arpegg.find()) {
				arp = Integer.parseInt(arpegg.group(0).replaceAll("\\{|\\}", ""));
			}
			
			int ret = 0;

			//detects chord change
			boolean chordChange = (s.indexOf(':') > 0) ? true : false;

			//cleans string
			String temp = s.replaceAll("[+|:]|\\(-?\\d*\\)", "");
			char[] arr = temp.toCharArray();

			int modifier = (Character.isUpperCase(arr[0])) ? -1 : 1;

			//switch stuff happens
			for (int i = 0; i < arr.length; i++) {
				if (i < arr.length - 1) {
					try {
						if (!((Character.isUpperCase(arr[0]) && Character.isUpperCase(arr[i + 1]))
								|| (Character.isLowerCase(arr[0]) && Character.isLowerCase(arr[i + 1])))) {
							throw new IntervalGroupingException();
						}
					} catch (IntervalGroupingException e) {
						e.printStackTrace();
					}
				}
				switch (Character.toLowerCase(arr[i])) {
				case 's':
					ret += SIXTEENTH * modifier;
					break;
				case 'e':
					ret += EIGHTH * modifier;
					break;
				case 'q':
					ret += QUARTER * modifier;
					break;
				case 'h':
					ret += HALF * modifier;
					break;
				case 'w':
					ret += WHOLE * modifier;
					break;
				}
			}

			event = new TimeEvent(ret, num);
			
			this.timeEvents.add(event);
			
			//chord changes are just a 0
			if (chordChange) {
				this.timeEvents.add(new TimeEvent(0, 0));
			}

		}
	}

	/**
	 * IS BASS??????
	 * @return BASSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
	 */
	public Boolean isBass() {
		return this.bass;
	}

	/**
	 * Gets regex matches.
	 * @return List of matches as strings.
	 */
	public ArrayList<String> getMatches() {
		return this.matches;
	}

	/**
	 * Gets time event queue.
	 * @return Queue of time events.
	 */
	public Queue<TimeEvent> getTimeEvents() {
		return this.timeEvents;
	}

	/**
	 * Time events spilled out as a string.
	 */
	public String toString() {
		return Arrays.toString(this.timeEvents.toArray());
	}

}
