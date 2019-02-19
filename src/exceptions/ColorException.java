package exceptions;

public class ColorException extends Exception{
	public ColorException() {
		super("Color added created chord confusion (ex. A9b9).");
	}
}
