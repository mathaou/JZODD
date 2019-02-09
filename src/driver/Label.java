package driver;

import java.io.Serializable;

/**
 * Object to write to and from file that holds label data.
 * @author Matt Farstad
 * @version 1.0
 */

public class Label implements Serializable {

	private static final long serialVersionUID = -2470348129462664588L;
	private String header, body;
	
	public Label(String header, String body) {
		this.header = header;
		this.body = body;
	}

	public String getHeader() {
		return header;
	}

	public String getBody() {
		return body;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	@Override
	public String toString() {
		return String.format("%s: {%s}", header, body);
	}

}
