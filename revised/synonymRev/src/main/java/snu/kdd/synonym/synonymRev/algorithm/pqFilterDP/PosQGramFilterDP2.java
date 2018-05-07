package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.util.Arrays;
import java.util.Random;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class PosQGramFilterDP2 {
	
	private final Record record;
	private final int q;
	private Boolean[][] bTransLen;
	private Boolean[][][] bGen;
	public int[] qgramPrefix;
	// bTransLen[i][l] indicates that s[1,i] can be transformed to a string of length l.
	
	public PosQGramFilterDP2(final Record record, final int q) {
		this.record = record;
		this.q = q;
		qgramPrefix = new int[q];
		computeTransformedLength();

		/*
		 * initialize:
		 * bGen[0][i][j] is true iff there is a transformed string of s[1,i] which ends with g[1,j] and "generates" [g[1,j], k].
		 * bGen[1][i][j] is true iff there is a transformed string o s[1,i] which "generates" [g[1,j], k].
		 */
		bGen = new Boolean[2][record.size()+1][q+1];
		for (int i=0; i<=record.size(); i++) Arrays.fill(  bGen[0][i], false );
		for (int i=0; i<=record.size(); i++) Arrays.fill(  bGen[1][i], false );
	}
	
	public Boolean existence(final int token, final int d, final int k) {
		/*
		 * Compute the existence of qgramPrefix[:d-1] + [token]. 
		 */
		
		final Boolean debug = false;
		
		// trivial case
		// Note that k starts from 0.
		if (record.getMaxTransLength() <= k ) return false;

		// bottom-up recursion at depth d.
		qgramPrefix[d-1] = token;
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
		if (debug) System.out.println( "["+Arrays.toString( qgramPrefix )+", "+k+"]" );
		if (debug) System.out.println(Arrays.deepToString(bTransLen).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[0]).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[1]).replaceAll( "],", "]\n" ));
		return bGen[1][record.size()][q];
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
				if ( Arrays.equals( Arrays.copyOfRange( qgramPrefix, j_start, j ), rule.getRight() ) ) if (bGen[0][i_back][j_start]) return true;
			}
			
			// Case 0-2
			if (isSuffixOf( qgramPrefix, 0, j, rule.getRight())) if(getBTransLen(i_back, k - (rule.rightSize() - j) )) return true;
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
				if ( isPrefixOf( qgramPrefix, j_start, j, rule.getRight() ) ) 
					if (bGen[0][i_back][j_start]) return true;
			}
			
			// Case 1-4
			int start = isSubstringOf( qgramPrefix, j, rule.getRight());
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
}