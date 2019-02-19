package exceptions;

public class InsufficientGrooveException extends Exception {
	public InsufficientGrooveException() {
		super("Groove durations must be equal.");
	}

}
