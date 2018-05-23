package snu.kdd.synonym.synonymRev.order;

import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;
import snu.kdd.synonym.synonymRev.tools.PosQGram;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class PositionFirstOrder extends AbstractGlobalOrder {
	
	public PositionFirstOrder( int qgramSize ) {
		super( qgramSize );
	}

	@Override
	public Ordering getMode() {
		return Ordering.PF;
	}

	public int getOrder( PosQGram o ) {
		return o.pos * nEntry + orderMap.get( o.qgram );
	}
	
	public int getOrder( int token, int pos ) {
		return pos * nEntry + orderMap.get(token);
	}

	public int compare( PosQGram o1, PosQGram o2 ) {
		int comp = Integer.compare( o1.pos, o2.pos );
		if ( comp != 0 ) return comp;
		else return Integer.compare( orderMap.get( o1.qgram ), orderMap.get( o2.qgram ) );
	}
	
	public int compare( int[] qgram1, int pos1, int[] qgram2, int pos2 ) {
		int comp = Integer.compare( pos1, pos2 );
		if ( comp != 0 ) return comp;
		else return Integer.compare( orderMap.get( new QGram(qgram1) ), orderMap.get( new QGram(qgram2) ) );
	}
	
	public int compare( int token1, int pos1, int token2, int pos2 ) {
		int comp = Integer.compare( pos1, pos2 );
		if ( comp != 0 ) return comp;
		else return Integer.compare( orderMap.get( token1 ), orderMap.get( token2 ) );
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
