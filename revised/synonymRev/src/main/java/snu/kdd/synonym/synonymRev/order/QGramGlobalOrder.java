//package snu.kdd.synonym.synonymRev.order;
//
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.List;
//
//import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import snu.kdd.synonym.synonymRev.data.Rule;
//import snu.kdd.synonym.synonymRev.tools.PosQGram;
//import snu.kdd.synonym.synonymRev.tools.QGram;
//
//public class QGramGlobalOrder extends AbstractGlobalOrderDeprecated<QGram> {
//	
//	public QGramComparator qgramComparator = new QGramComparator();
//	public PosQGramComparator pqgramComparator = new PosQGramComparator();
//	private final int qgramSize;
//	
//	public QGramGlobalOrder( String mode, int qgramSize) {
//		super( mode );
//		this.qgramSize = qgramSize;
//	}
//	
//	@Override
//	public long getOrder( QGram o ) {
//		throw new RuntimeException("Unexpected error");
//	}
//
//	@Override
//	protected List<QGram> parseRule( Rule rule, int pos ) {
//		int[] rhs = rule.getRight();
//		List<QGram> keyList = new ObjectArrayList<QGram>();
//		for ( int j=0; j<rhs.length+1-qgramSize; j++ )
//			keyList.add( new QGram( Arrays.copyOfRange( rhs, j, j+qgramSize )) );
//		return keyList;
//	}
//	
//	@Override
//	public int compare( QGram o1, QGram o2 ) {
//		switch (mode) {
//		case TF: return compareQGrams( o1.qgram, o2.qgram );
//		case FF: return Long.compare( orderMap.getLong( o1 ), orderMap.getLong( o2 ) );
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//	
//	public int compare( int[] qgram1, int[] qgram2 ) {
//		switch (mode) {
//		case TF: return compareQGrams( qgram1, qgram2 );
//		case FF: return Long.compare( orderMap.getLong(new QGram(qgram1)), orderMap.getLong(new QGram(qgram2)) );
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//	
//	public int compare( PosQGram o1, PosQGram o2 ) {
//		return comparePosQGrams( o1.qgram.qgram, o1.pos, o2.qgram.qgram, o2.pos );
//	}
//	
//	public int compareTokens(int token0, int token1) {
//		switch (mode) {
//		case TF: return Integer.compare( token0, token1 );
//		case FF: return Long.compare( orderMap.getLong( token0 ), orderMap.getLong( token1 ) );
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//	
//    private int compareQGrams( int[] qgram0, int[] qgram1 ) {
////        if ( qgram0.length == 1 && qgram1.length == 1 ) return compareTokens( qgram0[0], qgram1[0] );
//        int len = Math.min( qgram0.length, qgram1.length );
//        int res = Integer.MAX_VALUE;
//        for (int i=0; i<len; i++) {
//            res = Integer.compare( qgram0[i], qgram1[i] );
//            if (res != 0) return res;
//        }
//        if (qgram0.length > len) return 1;
//        else if (qgram1.length > len) return -1;
//        else return 0;
//    }
//
//	public int comparePosQGrams(int[] qgram0, int pos0, int[] qgram1, int pos1 ) {
//		int res = Integer.MAX_VALUE;
//		switch (mode) {
//		case PF:
//			res = Integer.compare( pos0, pos1 );
//			if (res != 0 ) return res;
//			else res = compareQGrams( qgram0, qgram1 );
//			break;
//
//		case TF:
//			res = compareQGrams( qgram0, qgram1 );
//			if (res != 0 ) return res;
//			else res = Integer.compare( pos0, pos1 );
//			break;
//
//		default:
//			throw new RuntimeException("UNIMPLEMENTED CASE");
//		}
//		assert res != Integer.MAX_VALUE;
//		return res;
//	}
//	
//	class QGramComparator implements Comparator<QGram> {
//		@Override
//		public int compare( QGram o1, QGram o2 ) {
//			return QGramGlobalOrder.this.compare( o1, o2 );
//		}
//	}
//	
//	class PosQGramComparator implements Comparator<PosQGram> {
//		@Override
//		public int compare( PosQGram o1, PosQGram o2 ) {
//			return QGramGlobalOrder.this.compare( o1, o2 );
//		}
//	}
//}
