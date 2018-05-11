package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.util.Arrays;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

public class PosQGramFilterDPInc extends PosQGramFilterDP implements IncrementalDP {
	
	protected int[] qgram;
	// bTransLen[i][l] indicates that s[1,i] can be transformed to a string of length l.
	
	public PosQGramFilterDPInc(final Record record, final int q) {
		super( record, q );
		qgram = new int[q];

		/*
		 * initialize:
		 * bGen[0][i][j] is true iff there is a transformed string of s[1,i] which ends with g[1,j] and "generates" [g[1,j], k].
		 * bGen[1][i][j] is true iff there is a transformed string o s[1,i] which "generates" [g[1,j], k].
		 */
		init_bGen();
	}
	
	@Override
	public Boolean existence(final int token, final int d, final int k) {
		/*
		 * Compute the existence of qgram[:d-1] + [token]. 
		 */
		
		final Boolean debug = false;
		
		// trivial case
		// Note that k starts from 0.
		if (record.getMaxTransLength() <= k ) return false;

		// bottom-up recursion at depth d.
		qgram[d-1] = token;
		for (int i=1; i<=record.size(); i++) {

			if (i == record.size() && token == Integer.MAX_VALUE) {
				bGen[0][i][d] = bGen[1][i][d] = bGen[0][i][d-1];
				continue;
			}
			
			// initialize.
			bGen[0][i][d] = bGen[1][i][d] = false;

			// re-compute.
			bGen[0][i][d] |= recursion0( k, i, d, bGen );
			bGen[1][i][d] |= recursion1( k, i, d, bGen );
		}
		if (debug) System.out.println( "["+Arrays.toString( qgram )+", "+k+"]" );
		if (debug) System.out.println(Arrays.deepToString(bTransLen).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[0]).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[1]).replaceAll( "],", "]\n" ));
		return bGen[1][record.size()][d];
	}

	private Boolean recursion0(final int k, final int i, final int j, final Boolean[][][] bGen) {
		/*
		 * Compute bGen[0][i][j] and return the result.
		 */
	
		for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
			int i_back = i - rule.leftSize();
			assert i_back >= 0;
			
			// Case 0-1
			// TODO: can be improved
			for (int j_start=0; j_start<j; j_start++) {
				if ( Arrays.equals( Arrays.copyOfRange( qgram, j_start, j ), rule.getRight() ) ) if (bGen[0][i_back][j_start]) return true;
			}
			
			// Case 0-2
			if (isSuffixOf( qgram, 0, j, rule.getRight())) if(getBTransLen(i_back, k - (rule.rightSize() - j) )) return true;
		}
		return false;
	}

	private Boolean recursion1(final int k, final int i, final int j, final Boolean[][][] bGen) {
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
				if ( isPrefixOf( qgram, j_start, j, rule.getRight() ) ) 
					if (bGen[0][i_back][j_start]) return true;
			}
			
			// Case 1-4
			int start = isSubstringOf( qgram, j, rule.getRight());
			if (start != -1 && getBTransLen(i_back, k-start)) return true;
	//		if (debug) System.out.println( "case 3: "+bGen[1][i][j]+"\t"+start);
	//		if (debug) System.out.println( Arrays.toString( qgram.qgram )+"\t"+Arrays.toString( rule.getRight() ));
		}
		return false;
	}

	@Override
	public int[] getQGram() {
		return qgram;
	}
}