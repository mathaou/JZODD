package exceptions;

public class FrequencyBoundsViolationException extends Exception{
	
	public FrequencyBoundsViolationException() {
		super("Note value not within valid MIDI range given root and octave.");
	}
}
