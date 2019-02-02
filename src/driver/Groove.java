package driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import exceptions.IntervalGroupingException;

public class Groove {

	public Queue<Integer> timeEvents;
	public ArrayList<String> matches;
	public Boolean bass;

	public static final int SIXTEENTH = 4;
	public static final int EIGHTH = 8;
	public static final int QUARTER = 16;
	public static final int HALF = 32;
	public static final int WHOLE = 64;

	public Groove(Boolean bass, String groove) {
		this.bass = bass;
		this.matches = new ArrayList<String>();
		this.timeEvents = new LinkedList<>();

		Matcher match = Pattern.compile("[seqhwSEQHW](\\+[seqwhSEQHW])*[:]?").matcher(groove);
		while (match.find()) {
			this.matches.add(match.group());
		}
		convertTime();
	}

	private void convertTime() {
		for (String s : this.matches) {

			int ret = 0;

			boolean chordChange = (s.indexOf(':') > 0) ? true : false;

			String temp = s.replaceAll("[+|:]", "");
			char[] arr = temp.toCharArray();

			int modifier = (Character.isUpperCase(arr[0])) ? -1 : 1;

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

			this.timeEvents.add(ret);
			if (chordChange) {
				this.timeEvents.add(0);
			}

		}
	}

	public Boolean isBass() {
		return this.bass;
	}

	public ArrayList<String> getMatches() {
		return this.matches;
	}

	public Queue<Integer> getTimeEvents() {
		return this.timeEvents;
	}

	public String toString() {
		return Arrays.toString(this.timeEvents.toArray());
	}

}
