package exceptions;

public class UndefinedChordException extends Exception{
	public UndefinedChordException() {
		super("Groove exists for nonexistant chord.");
	}
}
