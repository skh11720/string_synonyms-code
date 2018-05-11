package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.util.Arrays;
import java.util.Random;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class PosQGramFilterDPTopDown {
	
	private final Record record;
	private final int q;
	private Boolean[][] bTransLen;
	private QGram qgram;
	private int k;
	// bTransLen[i][l] indicates that s[1,i] can be transformed to a string of length l.
//	private Boolean[][][] bGen;
	private Object2BooleanOpenHashMap<IntTriple> bGen;
	
	public PosQGramFilterDPTopDown(final Record record, final int q) {
		this.record = record;
		this.q = q;
		bGen = new Object2BooleanOpenHashMap<IntTriple>();
		computeTransformedLength();
	}
	
	public Boolean existence(final QGram qgram, final int k) {
		/*
		 * Return true if the pos q-gram [qgram, k] is in TPQ of this.record; otherwise return false.
		 * Use dynamic programming to compute the matrix bGen
		 * whose element bGen[i][j] indicates that [qgram[1,j], k] is in TPQ of this.record[1,i].
		 * The return value is equal to bGen[record.size()][q].
		 */

		// trivial case
		// Note that k starts from 0.
		if (record.getMaxTransLength() <= k ) return false;
		
		this.qgram = qgram;
		this.k = k;
		bGen.clear();
		return existenceRecursive( record.size(), q, 1 );
	}
		
	
	@SuppressWarnings( "deprecation" )
	private Boolean existenceRecursive(final int i, final int j, final int o) {
		/*
		 * i, j starts from 1.
		 */
		// base cases
		if ( i <= 0 || j <= 0 ) return false;

		// memoization
		IntTriple key = new IntTriple(i, j, o);
		if ( bGen.containsKey( key ) ) return bGen.get( key );

		// recursion
		if ( i == record.size() && qgram.qgram[j-1] == Integer.MAX_VALUE )
			return existenceRecursive( i, j-1, 0 );

		if ( o == 0 ) {
			for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
				int i_back = i - rule.leftSize();
				assert i_back >= 0;

				// Case 0-1
				// TODO: can be improved
				for (int j_start=0; j_start<j; j_start++) {
					if ( Arrays.equals( Arrays.copyOfRange( qgram.qgram, j_start, j ), rule.getRight() ) ) {
						if ( existenceRecursive(i_back, j_start, o) ) return returnOutput( true, i, j, o );
					}
				}
				
				// Case 0-2
				if (isSuffixOf( qgram.qgram, 0, j, rule.getRight())) {
					if(getBTransLen(i_back, k - (rule.rightSize() - j) )) return returnOutput( true, i, j, o );
				}
			}
		}
		else { // o == 1
			for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
				int i_back = i - rule.leftSize();
				assert i_back >= 0;
				
				// Case 1-1
				if (existenceRecursive( i, j, 0 )) return returnOutput( true, i, j, o );
				
				// Case 1-2
				if (existenceRecursive( i_back, j, 1 )) return returnOutput( true, i, j, o );
				
				// Case 1-3
				// TODO: can be improved
				for (int j_start=0; j_start<j; j_start++) {
					if ( isPrefixOf( qgram.qgram, j_start, j, rule.getRight() ) ) 
						if (existenceRecursive( i_back, j_start, 0 )) return returnOutput( true, i, j, o );
				}
				
				// Case 1-4
				int start = isSubstringOf( qgram.qgram, j, rule.getRight());
				if (start != -1 && getBTransLen(i_back, k-start)) return returnOutput( true, i, j, o );
		//		if (debug) System.out.println( "case 3: "+bGen[1][i][j]+"\t"+start);
		//		if (debug) System.out.println( Arrays.toString( qgram.qgram )+"\t"+Arrays.toString( rule.getRight() ));
			}
		}
		return returnOutput( false, i, j, o );
	}
	
	@SuppressWarnings( "deprecation" )
	private Boolean returnOutput(final Boolean res, final int i, final int j, final int o) {
		bGen.put( new IntTriple(i, j, o), res );
		return res;
	}
	
	public void testIsSubstringOf() {
		System.out.println( "test PosQGramFilterDP.isSubstringOf()" );
		Random random = new Random();
		final int n_max = 10;
		final int nRepeat = 1000;
		for (int idx_case=0; idx_case<nRepeat; idx_case++) {
			final int len = random.nextInt(n_max)+1;
			int[] pat = new int[len];
			for (int i=0; i<len; ++i) pat[i] = random.nextInt( n_max );
			final int len_pre = random.nextInt( n_max );
			final int len_suf = random.nextInt( n_max );
			final int len_pos = len + len_pre + len_suf;
			final int len_neg = random.nextInt(n_max)+1;
			int[] seq_pos = new int[len+len_pre+len_suf];
			for (int i=0; i<len_pos; i++) {
				if (i >= len+len_pre || i < len_pre) seq_pos[i] = random.nextInt( n_max ) + n_max;
				else seq_pos[i] = pat[i-len_pre];
			}
			int[] seq_neg = new int[len_neg];
			for (int i=0; i<len_neg; i++) seq_neg[i] = random.nextInt( n_max ) + n_max;

//			System.out.println( len_pre+", "+len+", "+len_suf );
//			System.out.println( Arrays.toString( pat ) );
//			System.out.println( Arrays.toString( seq_pos ) );
//			System.out.println( Arrays.toString( seq_neg ) );
//			System.out.println( isSubstringOf( pat, seq_pos ) );
//			System.out.println( isSubstringOf( pat, seq_neg ) );
			
			assert isSubstringOf( pat, pat.length, seq_pos ) == len_pre;
			assert isSubstringOf( pat, pat.length, seq_neg ) == -1;
		}
	}
	
	public void testIsPrefixOf() {
		System.out.println( "test PosQGramFilterDp.isPrefixOf()" );
		Random random = new Random();
		final int n_max = 10;
		final int nRepeat = 1000;
		for (int idx_case=0; idx_case<nRepeat; idx_case++) {
			final int len_seq = random.nextInt( n_max )+1;
			int[] seq = new int[len_seq];
			for (int i=0; i<len_seq; i++) seq[i] = random.nextInt( n_max );

			final int len_pat = Math.min( random.nextInt( n_max )+1, len_seq);
			final int start = random.nextInt( n_max )+1;
			final int end = random.nextInt( n_max )+1;
			int[] pat = new int[start + len_pat + end];
			for (int i=0; i<start+len_pat+end; i++) {
				if (i < start || i >= start+len_pat) pat[i] = random.nextInt( n_max ) + n_max;
				else pat[i] = seq[i-start];
			}
			final int test_start = random.nextInt(start);
			final int test_end = random.nextInt(end)+start+len_pat;

//			System.out.println( len_pat+", "+start+", "+end );
//			System.out.println( Arrays.toString( seq ) );
//			System.out.println( Arrays.toString( pat ) );
//			System.out.println( test_start+", "+test_end );
//			System.out.println( isPrefixOf( pat, start, start+len_pat, seq ) );
//			System.out.println( isPrefixOf( pat, test_start, start+len_pat, seq ) );
//			System.out.println( isPrefixOf( pat, start, start+len_pat+test_end, seq ) );
//			System.out.println( isPrefixOf( pat, test_start, start+len_pat+test_end, seq ) );
			
			assert isPrefixOf( pat, start, start+len_pat, seq ) == true;
			assert isPrefixOf( pat, test_start, start+len_pat, seq ) == false;
			assert isPrefixOf( pat, start, start+len_pat+test_end, seq ) == false;
			assert isPrefixOf( pat, test_start, start+len_pat+test_end, seq ) == false;
		}
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
				if ( Arrays.equals( Arrays.copyOfRange( qgram.qgram, j_start, j ), rule.getRight() ) ) if (bGen[0][i_back][j_start]) return true;
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

	private Boolean getBTransLen(final int i, final int l) {
		if (l < 0) return false;
		else return bTransLen[i][l];
	}

	private void computeTransformedLength() {
		/*
		 * compute the matrix bTransLen using dynamic programming.
		 * the time complexity is |s|^2 * |R(s)|.
		 */
		// initialize
		bTransLen = new Boolean[record.size()][];
		for (int i=0; i<record.size(); i++) {
			bTransLen[i] = new Boolean[record.getMaxTransLength()];
			Arrays.fill( bTransLen[i], false );
		}
		bTransLen[0][0] = true;
		
		for (int i=1; i<record.size(); i++) {
			for (int l=1; l<record.getMaxTransLength(); l++ ) {
				for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
					int i_back = i - rule.leftSize();
					int l_back = l - rule.rightSize();
					//System.out.println( ""+i+", "+j+", "+i_back+", "+l_back+", "+rule.leftSize()+", "+rule.rightSize() );
					if (i_back >= 0 && l_back >= 0) bTransLen[i][l] |= bTransLen[i_back][l_back];
				}
			}
		}
//		System.out.println(Arrays.deepToString(bTransLen).replaceAll( "],", "]\n" ));
	}
	
	private Boolean isPrefixOf(final int[] pat, final int start, final int end, final int[] seq) {
		/*
		 * Return true if pat[start:end] is a prefix of seq; otherwise return false.
		 */
		if (end - start > seq.length ) return false;
		for (int i=start; i<end; i++)
			if (pat[i] != seq[i-start]) return false;
		return true;
	}

	private Boolean isSuffixOf(final int[] pat, final int start, final int end, final int[] seq) {
		/*
		 * Return true if pat[start:end] is a suffix of seq; otherwise return false.
		 */
		if (end - start > seq.length ) return false;
		for (int i=start; i<end; i++)
			if (pat[i] != seq[seq.length+i-end]) return false;
		return true;
	}
	
	private int isSubstringOf(final int[] pat, final int end, final int[] seq) {
		/*
		 * Return the start position of pat[start:end] if pat[start:end] is a substring of seq; otherwise return -1.
		 * Current implementation is naive: takes O(|pat|*|seq|).
		 * TODO: Need to be improved later!! (use AC automata)
		 */
		if (end > seq.length) return -1;
		for (int i=0; i<seq.length-end+1; i++) {
			Boolean res = true;
			for (int j=0; j<end; j++) {
				if (pat[j] != seq[i+j]) res = false;
				if (!res) break;
			}
			if (res) return i;
		}
		return -1;
	}
	
	private class IntTriple {
		int n1, n2, n3;
		private int hash;
		
		public IntTriple(int n1, int n2, int n3) {
			this.n1 = n1;
			this.n2 = n2;
			this.n3 = n3;
			hash = 0;
			hash = 0x1f1f1f1f ^ hash + n1;
			hash = 0x1f1f1f1f ^ hash + n2;
			hash = 0x1f1f1f1f ^ hash + n3;
		}
		
		@Override
		public int hashCode() {
			return this.hash;
		}
		
		@Override
		public boolean equals( Object obj ) {
			IntTriple o = (IntTriple)obj;
			if ( this.n1 == o.n1 && this.n2 == o.n2 && this.n3 == o.n3 ) return true;
			else return false;
		}
	}
}