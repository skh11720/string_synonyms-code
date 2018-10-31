package snu.kdd.synonym.synonymRev.tools;

public class QGramRange {

	public int min;
	public int max;
	public QGram qgram;

	public QGramRange( QGram qgram, int min, int max ) {
		this.qgram = qgram;
		this.min = min;
		this.max = max;
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}

		QGramRange oip = (QGramRange) o;

		if( hashCode() != oip.hashCode() ) {
			return false;
		}

		if( ( this.min == oip.min ) && ( this.max == oip.max ) ) {
			return this.qgram.equals( oip.qgram );
		}
		else {
			return false;
		}
	}
}
