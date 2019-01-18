package snu.kdd.synonym.synonymRev.order;

import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;
import snu.kdd.synonym.synonymRev.tools.PosQGram;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class PositionFirstOrder extends AbstractGlobalOrder {
	
	public PositionFirstOrder( int qSize ) {
		super( qSize );
	}

	@Override
	public Ordering getMode() {
		return Ordering.PF;
	}
	
	public int getOrder( PosQGram o ) {
		return o.pos * nEntry + orderMap.getInt( o.qgram );
	}
	
	public int getOrder( int token, int pos ) {
		return pos * nEntry + token;
	}

	public int compare( PosQGram o1, PosQGram o2 ) {
		int comp = Integer.compare( o1.pos, o2.pos );
		if ( comp != 0 ) return comp;
		else return Integer.compare( orderMap.getInt( o1.qgram ), orderMap.getInt( o2.qgram ) );
	}
	
	public int compare( int[] qgram1, int pos1, int[] qgram2, int pos2 ) {
		int comp = Integer.compare( pos1, pos2 );
		if ( comp != 0 ) return comp;
		else return Integer.compare( orderMap.getInt( new QGram(qgram1) ), orderMap.getInt( new QGram(qgram2) ) );
	}
	
	public int compare( int token1, int pos1, int token2, int pos2 ) {
		int comp = Integer.compare( pos1, pos2 );
		if ( comp != 0 ) return comp;
		else return Integer.compare( token1, token2 );
	}

	@Override
	public int getOrder( QGram o ) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public int compare( QGram o1, QGram o2 ) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public int compare( int[] qgram1, int[] qgram2 ) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public int getOrder( int token ) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public int compare( int token1, int token2 ) {
		throw new RuntimeException("Unimplemented");
	}
}
