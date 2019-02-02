package exceptions;

public class NoFunkException extends Exception {
	public NoFunkException() {
		super("Tried generating a file without groove on either hand.");
	}
}
