package exceptions;

public class IntervalGroupingException extends Exception{
	public IntervalGroupingException() {
		super("Grouping malformed (ex. Q+e || E+q+q+E), intervals should be of same case.");
	}
}
