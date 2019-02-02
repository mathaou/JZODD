package exceptions;

public class UndefinedRhtyhmException extends Exception{
	public UndefinedRhtyhmException() {
		super("Groove definition doesn't exist for chord(s).");
	}
}
