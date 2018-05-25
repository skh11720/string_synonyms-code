package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.util.Arrays;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.Util;

public class PosQGramFilterDP extends AbstractPosQGramFilterDP implements NaiveDP {
	
	protected Boolean[][][] bGen;
	
	public PosQGramFilterDP(final Record record, final int q) {
		super( record, q );
		bGen = new Boolean[2][record.size()+1][q+1];
	}
	
	public final Boolean existence(final QGram qgram, final int k) {
		/*
		 * Return true if the pos q-gram [qgram, k] is in TPQ of this.record; otherwise return false.
		 * Use dynamic programming to compute the matrix bGen
		 * whose element bGen[i][j] indicates that [qgram[1,j], k] is in TPQ of this.record[1,i].
		 * The return value is equal to bGen[record.size()][q].
		 */
		
		final Boolean debug = false;
		
		// trivial case
		if (record.getMaxTransLength() <= k ) return false;
		
		/*
		 * initialize:
		 * bGen[0][i][j] is true iff there is a transformed string of s[1,i] which ends with g[1,j] and "generates" [g[1,j], k].
		 * bGen[1][i][j] is true iff there is a transformed string o s[1,i] which "generates" [g[1,j], k].
		 */
		init_bGen();
		
		// bottom-up recursion
		for (int i=1; i<=record.size(); i++) {
			for (int j=1; j<=q; j++) {

				if (i == record.size() && qgram.qgram[j-1] == Integer.MAX_VALUE) {
					bGen[0][i][j] = bGen[1][i][j] = bGen[0][i][j-1];
					continue;
				}

				bGen[0][i][j] |= recursion0( qgram, k, i, j, bGen );
				bGen[1][i][j] |= recursion1( qgram, k, i, j, bGen );
			}
		}
		if (debug) System.out.println( "["+Arrays.toString( qgram.qgram )+", "+k+"]" );
		if (debug) System.out.println(Arrays.deepToString(bTransLen).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[0]).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[1]).replaceAll( "],", "]\n" ));
		return bGen[1][record.size()][q];
	}
	
	private Boolean recursion0(final QGram qgram, final int k, final int i, final int j, final Boolean[][][] bGen) {
		/*
		 * Compute bGen[0][i][j] and return the result.
		 */
	
		for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
			int i_back = i - rule.leftSize();
			assert i_back >= 0;
			
			// Case 0-1
			// TODO: can be improved
			for (int j_start=0; j_start<j; j_start++) {
				if ( Util.equalsToSubArray( qgram.qgram, j_start, j, rule.getRight() ) ) if (bGen[0][i_back][j_start]) return true;
			}
			
			// Case 0-2
			if (isSuffixOf( qgram.qgram, 0, j, rule.getRight())) if(getBTransLen(i_back, k - (rule.rightSize() - j) )) return true;
		}
		return false;
	}

	private Boolean recursion1(final QGram qgram, final int k, final int i, final int j, final Boolean[][][] bGen) {
		/*
		 * Compute bGen[1][i][j] and return the result.
		 */
	
		for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
			int i_back = i - rule.leftSize();
			assert i_back >= 0;
			
			// Case 1-1
			if (bGen[0][i][j]) return true;
			
			// Case 1-2
			if (bGen[1][i_back][j]) return true;
			
			// Case 1-3
			// TODO: can be improved
			for (int j_start=0; j_start<j; j_start++) {
				if ( isPrefixOf( qgram.qgram, j_start, j, rule.getRight() ) ) 
					if (bGen[0][i_back][j_start]) return true;
			}
			
			// Case 1-4
			int start = isSubstringOf( qgram.qgram, j, rule.getRight());
			if (start != -1 && getBTransLen(i_back, k-start)) return true;
	//		if (debug) System.out.println( "case 3: "+bGen[1][i][j]+"\t"+start);
	//		if (debug) System.out.println( Arrays.toString( qgram.qgram )+"\t"+Arrays.toString( rule.getRight() ));
		}
		return false;
	}
	
	protected void init_bGen() {
		for (int i=0; i<=record.size(); i++) Arrays.fill(  bGen[0][i], false );
		for (int i=0; i<=record.size(); i++) Arrays.fill(  bGen[1][i], false );
	}
}