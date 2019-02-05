<h1>JZODD MIDI BUILDER</h1>

**DESCRIPTION**

String-based MIDI composition written in Java. Modeled after how I personally create music, but there are applications for a lot of people. 

![Application window](https://i.imgur.com/BYyBWWs.png)

<h3>FUNCTIONALITY</h3>

**FORMAT**

Properly formatted chords should be separated with a | . Pressing space in JZODD automatically will create a bar for you, unless a repeat (x 2, x 3, etc...) or ) is detected.

Rhtyhms can be [seqhwSEQHW], with lowercase letters representing note values, and uppercase ones representing rests. Letters of same case can be joined with a + so long as total value doesn't equal w+w (currently all note values use bytes, and w+w == 128, which a byte can't hold). Rhythms should use a : to denote a chord change. Number of changes and number of chords must be equal!

Chords and rhythm can both be wrapped in parenthesis and repeated, as well as individual lines.

	ex. Ebmaj7/Ab:3^^ | Abmaj7:3 | x 2 (Ebmaj7/Ab:3^^ | Abmaj7:3 | x 2 Cmin7/F:4 | Fmin7:3^^ | x 2) x 2
		wW:qqQQ: x 2 (eeEeEessSsQq+q:w: x 2 w:w: x 2) x 2

Specific chord tones on the right hand can be voiced with parenthesis after the rhtyhm value. Values over or under the total number of chord tones will default to just the full chord.

	ex. w(1):w(2):w(3): / (lowest note to second lowest to third lowest)

**CHORDS AND QUALITY**

Chords can be defined with any root A-G, # | b, and with maj, min, aug, and dim qualities. No quality is the same as maj.

	ex. Cmin | Dbaug | Gmaj | Fdim | A |

**EXTENSIONS AND COLOR**

Extensions 7-13 in all of the harmonically logical places (no 8, 10, or 12). Maximum of 2 colors can be from 5-13 in similar places. Exceptions are in place and will output to canvas if chords or rhythm would produce malformed MIDI or are just illogical (such as a G#9b9 chord). Major quality + 7th will voice a major 7th, but any other quality will voice a dominant 7th.

	ex. Cmin7 | Dbaug6#9 | Gmaj9b5 | Fdim11#13b9 | A13b11#5 |

**BASS VOICING**

Bass voicing can be defined with a slash after the chord body. No defined voicing will default to the closest root note below the lowest chord tone.

	ex. Cmin7/G | Dbaug6#9/E | Gmaj9b5 | Fdim11#13b9/A | A13b11#5/Gb |
	
**OCTAVE AND INVERSION**

Octave and inversion ranges from A0 - C8. Outside the range will result in an exception. No octave defined will default to 4. Inversions are variable depending on number of chord tones. Some tones get removed depending on the extension in order to avoid dissonant intervals.

	if (this.extension == 7 || this.extension == 6) {
				if (this.inversion > 3) {
					throw new InvalidInversionException();
				}
			} else if (this.extension == 9) {
				if (this.inversion > 4) {
					throw new InvalidInversionException();
				}
			} else if (this.extension > 11) {
				if (this.inversion > 5) {
					throw new InvalidInversionException();
				}
			} else {
				if (this.inversion > 2) {
					throw new InvalidInversionException();
				}
			}
		
	ex. Cmin7/G:4^^^ | Dbaug6#9/E:^^^ | Gmaj9b5:^^^^ | Fdim11#13b9/A:7^ | A13b11#5/Gb:3 |

**FUTURE WORK**

Version 1.0 will have arpeggiate functionality, labels, save files, and visual representions of rhythm and chord changes on the currently underutilized but takes-up-more-than-half-of-the-screen canvas/ error output area.
