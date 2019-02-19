package exceptions;

public class InvalidInversionException extends Exception{
	
	public InvalidInversionException() {
		super("Inversion limit exceeded for extension given.");
	}
}
