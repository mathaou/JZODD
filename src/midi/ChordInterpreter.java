package midi;

public class ChordInterpreter {
	
	private char root;
	
	//C
	
	public ChordInterpreter(String chordSymbol) {
		this.root = chordSymbol.charAt(0);
	}
}
