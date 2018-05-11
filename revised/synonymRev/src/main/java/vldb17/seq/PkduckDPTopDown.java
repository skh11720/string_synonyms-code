package vldb17.seq;

import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import vldb17.ParamPkduck.GlobalOrder;

public class PkduckDPTopDown {
	
	public final GlobalOrder globalOrder;
	public final int len_max_S;

	protected Object2IntOpenHashMap<IntTriple> g;
	protected List<List<QGram>> availableQGrams;
	protected static final int inf = Integer.MAX_VALUE/2;
	private QGram target_qgram;
	private int k;
	private Record rec;
	
	// For debugging
	public PkduckDPTopDown( Record rec, GlobalOrder globalOrder, int len_max_S) {
		this.len_max_S = len_max_S;
		this.globalOrder = globalOrder;
		availableQGrams = rec.getSelfQGrams( 1, rec.size() );
		g = new Object2IntOpenHashMap<IntTriple>();
	}

	public PkduckDPTopDown( Record rec, JoinPkduck joinPkduck ) {
		this( rec, joinPkduck.globalOrder, joinPkduck.len_max_S );
	}
	
	public Boolean isInSigU( Record rec, QGram target_qgram, int k) {
		/*
		 * Compute g[o][i][l] for o=0,1, i=0~|rec|, l=0~max(|recS|).
		 * g[1][i][l] is X_l in the MIT paper.
		 */
		
		this.rec = rec;
		this.target_qgram = target_qgram;
		this.k = k;
		g.clear();

		for (int l=1; l<=len_max_S; l++) {
			int val = isInSigURecursive( rec.size(), l, 1 );
			if ( val == 0 ) return true;
		}
		return false;
	}

	public int isInSigURecursive( int i, int l, int o ) {
		/*
		 * i, l start from 1.
		 */

//		System.out.println( "PkduckIndex.isInSigU, "+target_qgram+", "+k );
		
		// base cases.
		if ( i <= 0 && l <= 0 && o == 0 ) return 0;
		if ( i <= 0 || l <= 0 ) return inf;
		
		// memoization.
		IntTriple key = new IntTriple( i, l, o );
		if ( g.containsKey( key ) ) return g.getInt( key );
		
		// recursion.
		QGram current_qgram = availableQGrams.get( i-1 ).get( 0 );
		int comp = comparePosQGrams( current_qgram.qgram, i-1, target_qgram.qgram, k );
		if ( o == 0 ) {
			// compute g[0][i][l].
//				System.out.println( "comp: "+comp );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			if ( comp != 0 ) update( isInSigURecursive( i-1, l-1, o ) + (comp==-1?1:0), i, l, o);
//				System.out.println( "g[0]["+(i-1)+"]["+(l-1)+"]: "+g[0][i-1][l-1] );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
//					System.out.println( rule );
				int[] rhs = rule.getRight();
				int num_smaller = 0;
				Boolean isValid = true;
				for (int j=0; j<rhs.length; j++) {
					// check whether the rule does not generate [target_token, k].
					isValid &= !(target_qgram.equals( Arrays.copyOfRange( rhs, j, j+1 ) ) && l-rhs.length+j == k); 
					num_smaller += comparePosQGrams( Arrays.copyOfRange( rhs, j, j+1 ), l-rhs.length+j, target_qgram.qgram, k )==-1?1:0;
				}
//					System.out.println( "isValid: "+isValid );
//					System.out.println( "num_smaller: "+num_smaller );
				if (isValid && i-rule.leftSize() >= 0 && l-rule.rightSize() >= 0) 
					update( isInSigURecursive( i-rule.leftSize(), l-rule.rightSize(), o ) + num_smaller, i, l, o);
			}
		}
		else { // o == 1
			// compute g[1][i][l].
//				System.out.println( "comp: "+comp );
			if ( comp != 0 ) update( isInSigURecursive( i-1, l-1, o ) + (comp==-1?1:0), i, l, o);
			else update( isInSigURecursive( i-1, l-1, 0 ), i, l, o);
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
			for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
//					System.out.println( rule );
				int[] rhs = rule.getRight();
				int num_smaller = 0;
				Boolean isValid = false;
				for (int j=0; j<rhs.length; j++) {
					// check whether the rule generates [target_token, k].
					isValid |= target_qgram.equals( Arrays.copyOfRange( rhs, j, j+1 ) ) && l-rhs.length+j == k;
					num_smaller += comparePosQGrams( Arrays.copyOfRange( rhs, j, j+1 ), l-rhs.length+j, target_qgram.qgram, k )==-1?1:0;
				}
//					System.out.println( "isValid: "+isValid );
//					System.out.println( "num_smaller: "+num_smaller );
				if ( i-rule.leftSize() >= 0 && l-rule.rightSize() >= 0) {
					update( isInSigURecursive( i-rule.leftSize(), l-rule.rightSize(), o ) + num_smaller, i, l, o );
					if (isValid) update( isInSigURecursive( i-rule.leftSize(), l-rule.rightSize(), 0 ) + num_smaller, i, l, o );
				}
			}
		}
		return g.getInt( key );
	}
	
	protected void update( int val, int i, int l, int o ) {
		IntTriple key = new IntTriple(i, l, o);
		if ( g.containsKey( key ) ) g.put( key, Math.min( val, g.getInt( key ) ) );
		else g.put( key, val );
	}

	protected int comparePosQGrams(int[] qgram0, int pos0, int[] qgram1, int pos1 ) {
		int res = Integer.MAX_VALUE;
		switch (globalOrder) {
		case PF:
			res = Integer.compare( pos0, pos1 );
			if (res != 0 ) return res;
			else res = PkduckIndex.compareQGrams( qgram0, qgram1 );
			break;

		case TF:
			res = PkduckIndex.compareQGrams( qgram0, qgram1 );
			if (res != 0 ) return res;
			else res = Integer.compare( pos0, pos1 );
			break;

		default:
			throw new RuntimeException("UNIMPLEMENTED CASE");
		}
		assert res != Integer.MAX_VALUE;
		return res;
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
