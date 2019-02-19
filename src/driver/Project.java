package driver;

import java.io.Serializable;

public class Project implements Serializable{

	private static final long serialVersionUID = -876528700049933257L;
	
	private String chords, lRhythm, rRhtyhm;
	
	public Project(String chords, String lRhythm, String rRhtyhm) {
		this.chords = chords;
		this.lRhythm = lRhythm;
		this.rRhtyhm = rRhtyhm;
	}

	public String getChords() {
		return chords;
	}

	public String getlRhythm() {
		return lRhythm;
	}

	public String getrRhtyhm() {
		return rRhtyhm;
	}

	public void setChords(String chords) {
		this.chords = chords;
	}

	public void setlRhythm(String lRhythm) {
		this.lRhythm = lRhythm;
	}

	public void setrRhtyhm(String rRhtyhm) {
		this.rRhtyhm = rRhtyhm;
	}
	
	

}
