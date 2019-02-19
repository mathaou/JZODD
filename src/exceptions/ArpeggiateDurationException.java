package exceptions;

public class ArpeggiateDurationException extends Exception {
	public ArpeggiateDurationException() {
		super("Defined arpeggiate note values exceeds parent note value.");
	}
}
