package exceptions;

public class TooMuchBassException extends Exception {
	public TooMuchBassException() {
		super("Only one voice can be bass.");
	}

}
