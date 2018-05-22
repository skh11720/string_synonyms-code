//package snu.kdd.synonym.synonymRev.order;
//
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.stream.Stream;
//
//import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
//import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
//import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import snu.kdd.synonym.synonymRev.data.Query;
//import snu.kdd.synonym.synonymRev.data.Record;
//import snu.kdd.synonym.synonymRev.data.Rule;
//import snu.kdd.synonym.synonymRev.tools.PosQGram;
//import snu.kdd.synonym.synonymRev.tools.PosQGram;
//
//public class PosQGramGlobalOrder extends AbstractGlobalOrder<PosQGram> {
//	
//	private final int qgramSize;
//	
//	public PosQGramGlobalOrder( String mode, int qgramSize) {
//		super( mode );
//		this.qgramSize = qgramSize;
//	}
//
//	@Override
//	protected List<PosQGram> parseRule( Rule rule, int pos ) {
//		int[] rhs = rule.getRigGht();
//		List<PosQGram> keyList = new ObjectArrayList<PosQGram>();
//		for ( int j=0; j<rhs.length+1-qgramSize; j++ )
//			keyList.add( new PosQGram( Arrays.copyOfRange( rhs, j, j+qgramSize )), pos-rule.leftSize() );
//		return keyList;
//	}
//	
//	@Override
//	public int compare( PosQGram o1, PosQGram o2 ) {
//		int[] qgram1 = o1.qgram;
//		int[] qgram2 = o2.qgram;
//		if ( qgram1.length == 1 && qgram2.length == 1 ) return compareTokens( qgram1[0], qgram2[0] );
//		int len = Math.min( qgram1.length, qgram2.length );
//		int res = Integer.MAX_VALUE;
//		for (int i=0; i<len; i++) {
//			res = Integer.compare( qgram1[i], qgram2[i] );
//			if (res != 0) return res;
//		}
//		if (qgram1.length > len) return 1;
//		else if (qgram2.length > len) return -1;
//		else return 0;
//	}
//	
//	public int compareTokens(int token0, int token1) {
//		switch (mode) {
//		case TF: return Integer.compare( token0, token1 );
//		case FF: return Integer.compare( orderMap.getInt( token0 ), orderMap.getInt( token1 ) );
//		default: throw new RuntimeException("Unexpected error");
//		}
//	}
//}
