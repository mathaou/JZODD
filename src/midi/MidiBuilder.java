package midi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import driver.ChordInterpreter;
import exceptions.LudicrousTempoException;
import exceptions.MalformedTimeSignatureException;
import javafx.stage.FileChooser;

public class MidiBuilder {

	public static MidiBuilder instance = new MidiBuilder();

	private ChordInterpreter interpreter;

	// Standard MIDI file header, for one-track file
	// 4D, 54... are just magic numbers to identify the
	// headers
	// Note that because we're only writing one track, we
	// can for simplicity combine the file and track headers
	private final int header[] = new int[] { 0x4d, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, // single-track
																										// format
			0x00, 0x01, // one track
			0x00, 0x10, // 16 ticks per quarter
			0x4d, 0x54, 0x72, 0x6B };

	// Standard footer
	private final int footer[] = new int[] { 0x01, 0xFF, 0x2F, 0x00 };

	// A MIDI event to set the tempo
	private int tempoEvent[] = new int[] { 0x00, 0xFF, 0x51, 0x03, 0x0F, 0x42, 0x40 // Default 1 million usec per
																							// crotchet
	};

	// A MIDI event to set the key signature. This is irrelent to
	// playback, but necessary for editing applications
	private final int keySigEvent[] = new int[] { 0x00, 0xFF, 0x59, 0x02, 0x00, // C
			0x00 // major
	};

	// A MIDI event to set the time signature. This is irrelent to
	// playback, but necessary for editing applications
	private int timeSigEvent[] = new int[] { 0x00, 0xFF, 0x58, 0x04, 0x04, // numerator
			0x02, // denominator (2==4, because it's a power of 2)
			0x30, // ticks per click (not used)
			0x08 // 32nd notes per crotchet
	};

	private Vector<int[]> playEvents;

	private MidiBuilder() {
		this.playEvents = new Vector<int[]>();
		this.interpreter = new ChordInterpreter();
	}

	//file writing finish
	public void writeToFile() throws IOException {
		
		FileChooser choose = new FileChooser();
		
		FileChooser.ExtensionFilter ext =  new FileChooser.ExtensionFilter("MIDI files (*.mid)", "*.mid");
		
		choose.getExtensionFilters().add(ext);

		File f = choose.showSaveDialog(null);
		
		if(f != null) {
			FileOutputStream fos = new FileOutputStream(f);
			
			fos.write(intArrayToByteArray(header));

			// Calculate the amount of track data
			// _Do_ include the footer but _do not_ include the
			// track header

			int size = tempoEvent.length + keySigEvent.length + timeSigEvent.length + footer.length;

			for (int i = 0; i < playEvents.size(); i++)
				size += playEvents.elementAt(i).length;

			// Write out the track data size in big-endian format
			// Note that this math is only valid for up to 64k of data
			// (but that's a lot of notes)
			int high = size / 256;
			int low = size - (high * 256);
			fos.write((byte) 0);
			fos.write((byte) 0);
			fos.write((byte) high);
			fos.write((byte) low);

			// Write the standard metadata — tempo, etc
			// At present, tempo is stuck at crotchet=60
			fos.write(intArrayToByteArray(tempoEvent));
			fos.write(intArrayToByteArray(keySigEvent));
			fos.write(intArrayToByteArray(timeSigEvent));

			// Write out the note, etc., events
			for (int i = 0; i < playEvents.size(); i++) {
				fos.write(intArrayToByteArray(playEvents.elementAt(i)));
			}

			// Write the footer and close
			fos.write(intArrayToByteArray(footer));
			fos.close();
		}
	}

	protected static byte[] intArrayToByteArray(int[] ints) {
		int l = ints.length;
		byte[] out = new byte[ints.length];
		for (int i = 0; i < l; i++) {
			out[i] = (byte) ints[i];
		}
		return out;
	}

	/** Store a program-change event at current position */
	public void progChange(int prog) {
		int[] data = new int[3];
		data[0] = 0;
		data[1] = 0xC0;
		data[2] = prog;
		playEvents.add(data);
	}
	
	public void setTempo(int bpm) throws LudicrousTempoException{
		if(bpm < 58 || bpm > 900) {
			throw new LudicrousTempoException();
		} else {
			String temp = Integer.toHexString(60000000/ bpm);
			instance.tempoEvent[4] = Integer.decode("0X0"+temp.charAt(0));
			instance.tempoEvent[5] = Integer.decode("0X"+temp.charAt(1) + "" + temp.charAt(2));
			instance.tempoEvent[6] = Integer.decode("0X"+temp.charAt(3) + "" + temp.charAt(4));
		}
	}
	
	@SuppressWarnings("unused")
	public void setTimeSignature(int n, int d) throws MalformedTimeSignatureException{
		if(d % 2 != 0) {
			throw new MalformedTimeSignatureException();
		} else {
			instance.tempoEvent[4] = n;
			instance.tempoEvent[5] = (int) log(d,2);
		}
	}
	
	public static double log(double x, int base) {
		return (Math.log(x) / Math.log(base));
	}	
		
	public ChordInterpreter getChordInterpreter() {
		return this.interpreter;
	}

	public Vector<int[]> getPlayEvents() {
		return this.playEvents;
	}

}
