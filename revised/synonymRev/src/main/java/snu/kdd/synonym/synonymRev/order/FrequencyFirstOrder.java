package snu.kdd.synonym.synonymRev.order;

import java.util.Arrays;
import java.util.Iterator;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
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
		return orderMap.getInt( o.qgram )*max_pos + o.pos;
	}
	
	public int getOrder( int token, int pos ) {
		return token*max_pos + pos;
	}

	public int compare( PosQGram o1, PosQGram o2 ) {
		int comp = Integer.compare( orderMap.getInt( o1.qgram ), orderMap.getInt( o2.qgram ) );
		if ( comp != 0 ) return comp;
		else return Integer.compare( o1.pos, o2.pos );
	}
	
	public int compare( int[] qgram1, int pos1, int[] qgram2, int pos2 ) {
		int comp = Integer.compare( orderMap.getInt( new QGram(qgram1) ), orderMap.getInt( new QGram(qgram2) ) );
		if ( comp != 0 ) return comp;
		else return Integer.compare( pos1, pos2 );
	}

	public int compare( int token1, int pos1, int token2, int pos2 ) {
		int comp = Integer.compare( token1, token2 );
		if ( comp != 0 ) return comp;
		else return Integer.compare( pos1, pos2 );
	}

	@Override
	public int getOrder( QGram o ) {
		return orderMap.getInt( o );
	}

	@Override
	public int compare( QGram o1, QGram o2 ) {
		return Integer.compare(  orderMap.getInt( o1 ), orderMap.getInt( o2 ) );
	}

	@Override
	public int compare( int[] qgram1, int[] qgram2 ) {
		return Integer.compare(  orderMap.getInt( new QGram(qgram1) ), orderMap.getInt( new QGram(qgram2) ) );
	}

	@Override
	public int getOrder( int token ) {
		return token;
	}

	@Override
	public int compare( int token1, int token2 ) {
		return Integer.compare( token1, token2 );
	}
}
