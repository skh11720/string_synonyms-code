package snu.kdd.synonym.synonymRev.order;

import snu.kdd.synonym.synonymRev.tools.PosQGram;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class FrequencyFirstOrder extends AbstractGlobalOrder {
	
	public FrequencyFirstOrder( int qgramSize ) {
		super( qgramSize );
	}
	
	@Override
	public Ordering getMode() {
		return Ordering.FF;
	}

	public int getOrder( PosQGram o ) {
		return orderMap.get( o.qgram );
	}
	
	public int getOrder( int token, int pos ) {
		return orderMap.get(token);
	}

	public int compare( PosQGram o1, PosQGram o2 ) {
		return Integer.compare( orderMap.get( o1.qgram ), orderMap.get( o2.qgram ) );
	}
	
	public int compare( int[] qgram1, int pos1, int[] qgram2, int pos2 ) {
		return Integer.compare( orderMap.get( new QGram(qgram1) ), orderMap.get( new QGram(qgram2) ) );
	}
	
	// TOO SLOW!!
	public int compare( int token1, int pos1, int token2, int pos2 ) {
		return Integer.compare( orderMap.get( token1 ), orderMap.get( token2 ) );
	}

	@Override
	public int getOrder( QGram o ) {
		return orderMap.get( o );
	}

	@Override
	public int compare( QGram o1, QGram o2 ) {
		return Integer.compare(  orderMap.get( o1 ), orderMap.get( o2 ) );
	}

	@Override
	public int compare( int[] qgram1, int[] qgram2 ) {
		return Integer.compare(  orderMap.get( new QGram(qgram1) ), orderMap.get( new QGram(qgram2) ) );
	}

	@Override
	public int getOrder( int token ) {
		return orderMap.get( token );
	}

	@Override
	public int compare( int token1, int token2 ) {
		return Integer.compare( orderMap.get( token1 ), orderMap.get( token2 ) );
	}
}
