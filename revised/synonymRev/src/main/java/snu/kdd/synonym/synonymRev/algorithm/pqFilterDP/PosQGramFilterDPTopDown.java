package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.util.Arrays;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class PosQGramFilterDPTopDown extends AbstractPosQGramFilterDP {
	
	public int[] qgram;
	protected int k;
	protected Object2BooleanOpenHashMap<IntTriple> bGen;
//	private QGram qgram;
	// bTransLen[i][l] indicates that s[1,i] can be transformed to a string of length l.
//	private Boolean[][][] bGen;
	
	public PosQGramFilterDPTopDown(final Record record, final int q) {
		super( record, q );
		bGen = new Object2BooleanOpenHashMap<IntTriple>();
	}
	
	public final Boolean existence(final QGram qgram, final int k) {
		/*
		 * Return true if the pos q-gram [qgram, k] is in TPQ of this.record; otherwise return false.
		 * Use dynamic programming to compute the matrix bGen
		 * whose element bGen[i][j] indicates that [qgram[1,j], k] is in TPQ of this.record[1,i].
		 * The return value is equal to bGen[record.size()][q].
		 */

		// trivial case
		// Note that k starts from 0.
		if (record.getMaxTransLength() <= k ) return false;
		
		this.qgram = qgram.qgram.clone();
		this.k = k;
		bGen.clear();
		return existenceRecursive( record.size(), q, 1 );
	}
	
	protected Boolean existenceRecursive(final int i, final int j, final int o) {
		/*
		 * i, j start from 1.
		 */
		// base cases
		if ( i <= 0 || j <= 0 ) return false;

		// memoization
		IntTriple key = new IntTriple(i, j, o);
		if ( bGen.containsKey( key ) ) return bGen.getBoolean( key );

		// recursion
		if ( i == record.size() && qgram[j-1] == Integer.MAX_VALUE )
			return existenceRecursive( i, j-1, 0 );

		if ( o == 0 ) {
			for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
				int i_back = i - rule.leftSize();
				assert i_back >= 0;

				// Case 0-1
				// TODO: can be improved
				for (int j_start=0; j_start<j; j_start++) {
					if ( Arrays.equals( Arrays.copyOfRange( qgram, j_start, j ), rule.getRight() ) ) {
						if ( existenceRecursive(i_back, j_start, o) ) return returnOutput( true, i, j, o );
					}
				}
				
				// Case 0-2
				if (isSuffixOf( qgram, 0, j, rule.getRight())) {
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
					if ( isPrefixOf( qgram, j_start, j, rule.getRight() ) ) 
						if (existenceRecursive( i_back, j_start, 0 )) return returnOutput( true, i, j, o );
				}
				
				// Case 1-4
				int start = isSubstringOf( qgram, j, rule.getRight());
				if (start != -1 && getBTransLen(i_back, k-start)) return returnOutput( true, i, j, o );
		//		if (debug) System.out.println( "case 3: "+bGen[1][i][j]+"\t"+start);
		//		if (debug) System.out.println( Arrays.toString( qgram)+"\t"+Arrays.toString( rule.getRight() ));
			}
		}
		return returnOutput( false, i, j, o );
	}
	
	private Boolean returnOutput(final Boolean res, final int i, final int j, final int o) {
		bGen.putIfAbsent( new IntTriple(i, j, o), res );
		return res;
	}
	
	protected class IntTriple {
		private final int n1, n2, n3;
		private final int hash;
		
		public IntTriple(int n1, int n2, int n3) {
			this.n1 = n1;
			this.n2 = n2;
			this.n3 = n3;
			int hash = 0;
			hash = 0x1f1f1f1f ^ hash + n1;
			hash = 0x1f1f1f1f ^ hash + n2;
			hash = 0x1f1f1f1f ^ hash + n3;
			this.hash = hash;
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
		
		@Override
		public String toString() {
			return "("+n1+", "+n2+", "+n3+")";
		}
	}
}