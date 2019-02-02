package exceptions;

public class MalformedTimeSignatureException extends Exception{
	public MalformedTimeSignatureException() {
		super("Denominator was not a power of two.");
	}
}
