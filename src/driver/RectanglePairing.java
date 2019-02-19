package driver;

public class RectanglePairing<Color, Rectangle> {

	public Color c;
	public Rectangle r;
	
	public RectanglePairing(Color c, Rectangle r) {
		this.c = c;
		this.r = r;
	}

	public Color getC() {
		return c;
	}

	public Rectangle getR() {
		return r;
	}

	public void setC(Color c) {
		this.c = c;
	}

	public void setR(Rectangle r) {
		this.r = r;
	}

}
