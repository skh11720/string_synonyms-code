package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.Util;

public class PosQGramFilterDPDelta extends AbstractPosQGramFilterDP {
	
	public static final int INF = Integer.MAX_VALUE;
	private Boolean[][][][] bGen;
	private Boolean debug = false;
	//final int deltaMax;
	private int deltaMax;
	
	public PosQGramFilterDPDelta(final Record record, final int q, final int deltaMax ) {
		super( record, q );
		// TODO: reduce the number of memory allocations
		bGen = new Boolean[2][deltaMax+1][record.size()+1][q+1];
		this.deltaMax = deltaMax;
	}
	
	public final Boolean existence(final QGram qgram, final int k, final int deltaMax) {
		/*
		 * Return true if the pos q-gram [qgram, k] is in TPQ of this.record; otherwise return false.
		 * Use dynamic programming to compute the matrix bGen
		 * whose element bGen[i][j] indicates that [qgram[1,j], k] is in TPQ of this.record[1,i].
		 * The return value is equal to bGen[record.size()][q].
		 */
		
		debug = false;
		this.deltaMax = deltaMax;
//		if ( record.getID() == 1458 && Arrays.equals( qgram.qgram, new int[] {27840, 21051, 4788} )) debug = true;
		
		// trivial cases
		// handle qgrams with INF only
		if ( qgram.qgram[0] == INF ) {
//			System.out.println( (record.getMinTransLength()-deltaMax) + ", " +k );
			if ( record.getMinTransLength()-deltaMax > k ) return false;
			else return true;
//			else {
//				for ( int l=1; l<bTransLen.length; ++l ) 
//					if ( getBTransLen( record.size()-1, l ) ) return true;
//			}
		}
		if (record.getMaxTransLength() <= k ) return false;
		
		
		
		/*
		 * initialize:
		 * bGen[0][i][j] is true iff there is a transformed string of s[1,i] which ends with g[1,j] and "generates" [g[1,j], k].
		 * bGen[1][i][j] is true iff there is a transformed string o s[1,i] which "generates" [g[1,j], k].
		 */
		init_bGen();
		setValidRules(qgram);
//		System.out.println( "valid rules of size "+validRules.size()+":" );
//		for ( int pos : validRules.keySet() ) {
//			System.out.println( "pos: "+pos+", "+validRules.get( pos ) );
//			if ( validRules.get( pos ) == null ) continue;
//			for ( Rule rule : validRules.get( pos ) ) {
//				System.out.println( "\t"+pos+", "+rule );
//				
//			}
//		}
		
		// bottom-up recursion
		for (int d=0; d<=deltaMax; ++d ) {
			for (int i=1; i<=record.size(); i++) {
				for (int j=1; j<=q; j++) {
					// case 0-1, case 1-2
					if ( d > 0 ) {
						bGen[0][d][i][j] |= bGen[0][d-1][i][j];
						bGen[1][d][i][j] |= bGen[1][d-1][i][j];
					}
//					if (i == record.size() && qgram.qgram[j-1] == Integer.MAX_VALUE) {
					if (qgram.qgram[j-1] == Integer.MAX_VALUE) {
//						bGen[0][d][i][j] |= bGen[0][d][i][j-1];
						bGen[1][d][i][j] |= bGen[0][d][i][j-1];
						if ( j > 1 && qgram.qgram[j-2] == Integer.MAX_VALUE ) bGen[1][d][i][j] |= bGen[1][d][i][j-1];
					}

					if ( !bGen[0][d][i][j] ) bGen[0][d][i][j] |= recursion0( qgram, k, i, j, d, bGen );
					if ( !bGen[1][d][i][j] ) bGen[1][d][i][j] |= recursion1( qgram, k, i, j, d, bGen );
				}
			}
		}
		if (debug) System.out.println( "["+Arrays.toString( qgram.qgram )+", "+k+"]" );
		if (debug) System.out.println(Arrays.deepToString(bTransLen).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[0]).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[1]).replaceAll( "],", "]\n" ));
		return bGen[1][deltaMax][record.size()][q];
	}
	
	private void init_bGen() {
		for (int d=0; d<=deltaMax; ++d ) {
			for (int i=0; i<=record.size(); i++) Arrays.fill(  bGen[0][d][i], false );
			for (int i=0; i<=record.size(); i++) Arrays.fill(  bGen[1][d][i], false );
		}
	}
	
	private Boolean recursion0(final QGram qgram, final int k, final int i, final int j, final int d, final Boolean[][][][] bGen) {
		/*
		 * Compute bGen[0][d][i][j] and return the result.
		 */
		
		for (final Rule rule : record.getSuffixApplicableRules( i-1 ) ) {
			int i_back = i - rule.leftSize();
			assert i_back >= 0;
			
			// Case 0-2: Rightmost errors
			if ( rule.rightSize() <= d && bGen[0][d-rule.rightSize()][i_back][j] ) return true;

			// Case 0-3: Matching a suffix of the qgram
			for (int j_start=1; j_start<j; j_start++) {
				int d0 = alignWithSeq( qgram.qgram, j_start, j, rule.getRight() );
				if ( (d0 <= d) && bGen[0][d-d0][i_back][j_start] ) return true;
			}
			
			// Case 0-4: Matching the whole qgram
//			System.out.println( d+", "+i+", "+j+", "+rule+", "+m+", "+i_back+", "+(k-m)+", "+getBTransLen( i_back, k-m ) );
			boolean bCase04 = alignWithSuffix2( qgram.qgram, 0, j, rule.getRight(), d, i_back, k );
			if (bCase04) return true;
		}
		return false;
	}

	private Boolean recursion1(final QGram qgram, final int k, final int i, final int j, final int d, final Boolean[][][][] bGen) {
		/*
		 * Compute bGen[1][d][i][j] and return the result.
		 */
//		System.out.println( "recursion1: "+d+", "+i+", "+j );
	
		for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
			int i_back = i - rule.leftSize();
			assert i_back >= 0;
			
			// Case 1-2: Skip useless rule
			if (qgram.qgram[j-1] != INF && bGen[1][d][i_back][j]) {
//				System.out.println( "recursion1, case 1-2: "+d+", "+i+", "+j );
				return true;
			}
			
			// Case 1-3: Rightmost errors
			if ( rule.rightSize() <= d && bGen[1][d-rule.rightSize()][i-rule.leftSize()][j] ) {
//				System.out.println( "recursion1, case 1-3: "+d+", "+i+", "+j );
				return true;
			}
		}	

		if ( !validRules.containsKey( i-1 ) ) {
//			System.out.println( "recursion1, no valid rules: "+d+", "+i+", "+j );
			return false;
		}
		for (final Rule rule : validRules.get( i-1 )) {
			int i_back = i - rule.leftSize();
			// Case 1-4: Matching a suffix of the qgram
			for (int j_start=1; j_start<j; j_start++) {
				int d1 = alignWithPrefix( qgram.qgram, j_start, j, rule.getRight(), 0 );
				// is it necessary to enumerate all d2? YES.
				for ( int d2=0; d2<=d-d1; ++d2 ) {
					if ( bGen[0][d-d1-d2][i_back][j_start] ) {
//						System.out.println( "recursion1, case 1-4: "+d+", "+i+", "+j );
						return true;
					}
				}
			}
			
			// Case 1-5: Matching the whole qgram
			for ( int m=0; m<rule.rightSize(); ++m ) {
				int d1 = alignWithPrefix( qgram.qgram, 0, j, rule.getRight(), m );
				if ( d1 > d ) continue;
				if ( d == 0 ) {
					if ( getBTransLen(i_back, k-m) ) return true;
				}
				/*
				 *  Given d budgets, d1 is used to align the qgram with the rhs of the current rule.
				 *  Thus, the remaining budgets d-d1 can be used to "set" the starting position of the qgram to "k".
				 *  Note that m is the number of tokens on the left side of the qgram.
				 *  If m + (transformed length of s[1,i-back]) - (additional errors, 0 from d-d1) is k, this case holds and return true. (k is zero-based.)
				 *  In other words, k - m + (d-d1) = (transformed length of s[1,i-back]) 
				 */
				else {
					for ( int d2=0; d2<=d-d1; ++d2 ) {
						if ( k-m+d2 >= bTransLen[0].length ) break;
						if ( getBTransLen(i_back, k-m+d2) ) return true;
					}
//					int lb = m + (i_back > 0? record.getTransLengthsAll()[i_back-1][0]: 0) - (d-d1);
//					int ub = m + (i_back > 0? record.getTransLengthsAll()[i_back-1][1]: 0);
//					if ( lb <= k && k <= ub ) {
////						System.out.println( "recursion1, case 1-5: "+d+", "+i+", "+j );
////						System.out.println( "d1: "+d1+", lb: "+lb+", ub: "+ub );
//						return true;
//					}
				}
			}
	//		if (debug) System.out.println( "case 3: "+bGen[1][i][j]+"\t"+start);
	//		if (debug) System.out.println( Arrays.toString( qgram.qgram )+"\t"+Arrays.toString( rule.getRight() ));
		}
		return false;
	}
	
	protected int alignWithSeq( int[] pat, int start, int end, int[] seq ) {
		/*
		 * Return the minimum number of errors required to match pat[start:end] and seq.
		 * If the number of errors is larger than deltaMax, return INF.
		 */
		if ( end - start > seq.length ) return INF;
		int d = 0;
		int i = end-1;
		int j = seq.length-1;
		for (; i>=start && j>=0; --j ) {
//			System.out.println( i+", "+pat[i]+", "+j+", "+seq[j]+", "+d );
			if ( pat[i] != seq[j] ) ++d;
			else --i;
			if ( d > deltaMax ) return INF;
		}
		if ( i >= start ) return INF;
		d += i + j + 2 - start;
		return d > deltaMax ? INF : d;
	}
	
	protected int alignWithSuffix( int[] pat, int start, int end, int[] seq, int delta ) {
		/*
		 * Match pat[start:end] to a suffix of seq with at most delta errors.
		 * Return the starting position of the suffix.
		 * If the matching fails, return -1.
		 */
		if ( end - start > seq.length ) return -1;
		int d = delta;
		int i = end-1;
		int j = seq.length-1;
		for (; i>=start; --j ) {
			if ( j < 0 ) return -1;
			if ( pat[i] != seq[j] ) {
				if ( d <= 0 ) return -1;
				--d;
			}
			else --i;
		}
		return j+1;
	}

	protected boolean alignWithSuffix2( int[] pat, int start, int end, int[] seq, int delta, int i_back, int k ) {
		/*
		 * Match pat[start:end] to a suffix of seq with at most delta errors.
		 * Let m be the starting position of such a suffix in seq.
		 * Check if the remaining part of the input string can be transformed to a string of length k-m.
		 * Then, return true, otherwise false.
		 */
		if ( end - start > seq.length ) return false;
		int d = delta;
		int i = end-1;
		int j = seq.length-1;
		for (; i>=start; --j ) {
			if ( j < 0 ) return false;
			if ( pat[i] != seq[j] ) {
				if ( d <= 0 ) return false;
				--d;
			}
			else --i;
		}
		
//		int m = j+1;
		for ( int d0=0; d0<=d; ++d0 ) { // consume the remaining d.
			if ( k-j-1+d0 >= bTransLen[0].length ) continue;
			if ( k-j-1+d0 < 0 ) continue;
			if ( bTransLen[i_back][k-j-1+d0] ) return true;
		}
		return false;
	}
	
	protected int alignWithPrefix( int[] pat, int pat_start, int end, int[] seq, int seq_start ) {
		/*
		 * Math pat[start:end] to a prefix of seq[seq_start:] with at most deltaMax errors.
		 * Return the minimum number of errors required to match.
		 * If the number of errors is larger than deltaMax, return INF.
		 */
		if ( end - pat_start > seq.length - seq_start ) return INF;
		int d = 0;
		int i = pat_start;
		int j = seq_start;
		for ( ; i<end; ++j ) {
			if ( pat[i] == INF && j >= seq.length) break;
			if ( j >= seq.length || d > deltaMax ) return INF;
			if ( pat[i] != seq[j] ) ++d;
			else ++i;
		}
		return d > deltaMax ? INF : d;
	}
	
	public void printBTransLen() {
		System.out.println( "bTransLen" );
		for ( int i=0; i<bTransLen.length; ++i ) {
			for ( int j=0; j<bTransLen[0].length; ++j ) {
				System.out.print( bTransLen[i][j] ? "o  " : "x  " );
			}
			System.out.println( "" );
		}
		System.out.println( "" );
	}
	
	public void printBGen() {
		if ( bGen[0][0][0][0] == null ) {
			System.out.println( "null" );
			return;
		}
		for (int o=0; o<2; ++o ) {
			System.out.println( "bGen["+o+"]" );
			for ( int i=0; i<=record.size(); ++i ) {
				for ( int d=0; d<=deltaMax; ++d ) {
					for ( int j=0; j<=q; ++j ) {
						System.out.print( bGen[o][d][i][j] ? "o  " : "x  " );
					}
					System.out.print( "   " );
				}
				System.out.println( "" );
			}
			System.out.println( "" );
		}
	}
}
