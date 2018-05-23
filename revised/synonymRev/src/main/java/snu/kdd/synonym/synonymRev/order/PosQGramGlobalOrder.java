//package snu.kdd.synonym.synonymRev.order;
//
//import java.util.List;
//
//import snu.kdd.synonym.synonymRev.data.Query;
//import snu.kdd.synonym.synonymRev.data.Rule;
//import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;
//import snu.kdd.synonym.synonymRev.tools.PosQGram;
//
//@SuppressWarnings("unchecked")
//public class PosQGramGlobalOrder extends AbstractGlobalOrderDeprecated<PosQGram> {
//	
//	private final int qgramSize;
//	@SuppressWarnings( "rawtypes" )
//	private AbstractGlobalOrderDeprecated underlyingOrder = null;
//	private long underlyingSize;
//	
//	public PosQGramGlobalOrder( String mode, int qgramSize) {
//		super( mode );
//		if ( Ordering.valueOf( mode )== Ordering.TF ) throw new RuntimeException("PosQGramGlobalOrder currently does not support TF.");
//		this.qgramSize = qgramSize;
//		if ( this.qgramSize == 1 ) underlyingOrder = new TokenGlobalOrder( "FF" );
//		else underlyingOrder = new QGramGlobalOrder( "FF", qgramSize );
//	}
//	
////	@Override
//	public void initOrder( Query query ) {
//		if ( mode == Ordering.TF ) return;
//		underlyingOrder.initOrder( query );
//		underlyingSize = underlyingOrder.orderMap.size();
//	}
//
//	@Override
//	public long getOrder( PosQGram o ) {
//		switch (mode) {
//		case PF: return o.pos * underlyingSize + underlyingOrder.getOrder(o.qgram);
//		case TF:
//		case FF:
//			return underlyingOrder.getOrder( o.qgram );
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//	
//	// Variation 1 of getOrder
//	public long getOrder( int token, int pos ) {
//		switch (mode) {
//		case PF: return pos * underlyingSize + underlyingOrder.getOrder(token);
//		case TF:
//		case FF:
//			return underlyingOrder.getOrder( token );
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//
//	@Override
//	protected List<PosQGram> parseRule( Rule rule, int pos ) {
//		throw new RuntimeException("Unexpected error");
//	}
//	
//	@Override
//	public int compare( PosQGram o1, PosQGram o2 ) {
//		switch (mode) {
//		case PF: {
//			int comp = Integer.compare( o1.pos, o2.pos );
//			if ( comp != 0 ) return comp;
//		}
//		case TF:
//		case FF:
//			if ( qgramSize == 1 ) return underlyingOrder.compare( o1.qgram.qgram[0], o2.qgram.qgram[0] );
//			else return underlyingOrder.compare( o1.qgram, o2.qgram );
//
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//	
//	// Variation 1 of compare
//	public int compare( int[] qgram1, int pos1, int[] qgram2, int pos2 ) {
//		switch (mode) {
//		case PF: {
//			int comp = Integer.compare( pos1, pos2 );
//			if ( comp != 0 ) return comp;
//		}
//		case TF:
//		case FF:
//			if ( qgramSize == 1 ) return underlyingOrder.compare( qgram1[0], qgram2[0] );
//			else return underlyingOrder.compare( qgram1, qgram2 );
//
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//	
//	// Variation 2 of compare
//	public int compare( int token1, int pos1, int token2, int pos2 ) {
//		switch (mode) {
//		case PF: {
//			int comp = Integer.compare( pos1, pos2 );
//			if ( comp != 0 ) return comp;
//		}
//		case TF:
//		case FF:
//			if ( qgramSize == 1 ) return underlyingOrder.compare( token1, token2 );
//
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//}
