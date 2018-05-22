package snu.kdd.synonym.synonymRev.tools;

public class PosQGram {
	public final QGram qgram;
	public final int pos; // 0-based
	private final int hash;
	
	public PosQGram( QGram qgram, int pos ) {
		this.qgram = qgram;
		this.pos = pos;
		int hash = 0;
		hash = 0x1f1f1f1f ^ hash + qgram.hashCode();
		hash = 0x1f1f1f1f ^ hash + pos;
		this.hash = hash;
	}
	
	public PosQGram( int[] qgram, int pos ) {
		this( new QGram( qgram ), pos );
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) return false;
		PosQGram o = (PosQGram)obj;
		return ( qgram.equals( o.qgram ) && pos == o.pos );
	}
	
	@Override
	public String toString() {
		return "["+qgram.toString()+", "+pos+"]";
	}
}
