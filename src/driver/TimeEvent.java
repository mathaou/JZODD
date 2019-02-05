package driver;

/**
 * Simple object to hold note modifications and time as integer in one place.
 * @author Matt Farstad
 * @version 1.0
 */
public class TimeEvent {

	/**
	 * @param te Note value as Integer.
	 */
	private Integer te;
	
	/**
	 * @param num Note modification.
	 */
	private int num;
	
	/**
	 * Constructor.
	 * @param te 
	 * @param num
	 */
	public TimeEvent(Integer te, int num) {
		this.te = te;
		this.num = num;
	}
	
	public Integer getTimeEvent() {
		return this.te;
	}
	
	public int getNum() {
		return this.num;
	}

}
