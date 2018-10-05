package vldb17.set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.IntTriple;

public class PkduckSetDPTopDown {
	
	protected final AbstractGlobalOrder globalOrder;
	protected final int len_max_S;
	protected final Record rec;
	protected final int[] tokens;
	protected Object2IntOpenHashMap<IntTriple> mem;
	protected int target;
	
	protected static final int inf = Integer.MAX_VALUE/2;

	protected static int safeAdd( int a, int b ) {
		if ( a > 0 && b > 0 && a+b < 0 ) // positive overflow detected
			return Integer.MAX_VALUE;
		else return a+b;
	}
	
	public PkduckSetDPTopDown( Record rec, AbstractGlobalOrder globalOrder ) {
		this.rec = rec;
		this.tokens = rec.getTokensArray();
		this.len_max_S = rec.getMaxTransLength();
		this.globalOrder = globalOrder;
		this.mem = new Object2IntOpenHashMap<>();
	}
	
	public Boolean isInSigU( int target ) {
		this.target = target;
		this.mem.clear();
		for (int l=1; l<=len_max_S; l++) {
			if ( isInSigURecursive( 1, rec.size(), l ) == 0 ) return true;
		}
		return false;
	}
	
	protected int isInSigURecursive( int o, int i, int l ) {
		// base cases
		if ( i <= 0 && l <= 0 && o == 0 ) return 0;
		if ( i <= 0 || l <= 0 ) return inf;
		
		// memoization
		IntTriple key = new IntTriple( o, i, l );
		if ( mem.containsKey( key ) ) return mem.getInt( key );
		
		// recursion
		final int token = tokens[i-1];
		final int comp = globalOrder.compare( token, target );
		int val = inf;
		if ( o == 0 ) {
			if ( comp != 0 ) val = Math.min( val, safeAdd( isInSigURecursive( 0, i-1, l-1 ), (comp<0?1:0) ) );
			for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
				int[] rhs = rule.getRight();
				int num_smaller = 0;
				Boolean isValid = true;
				for (int j=0; j<rhs.length; j++) {
					// check whether the rule does not generate [target_token, k].
					isValid &= (target != rhs[j]);
					num_smaller += globalOrder.compare( rhs[j], target )<0?1:0;
				}
				if (isValid && i-rule.leftSize() >= 0 && l-rule.rightSize() >= 0) 
					val = Math.min( val, safeAdd( isInSigURecursive( 0, i-rule.leftSize(), l-rule.rightSize() ), num_smaller ) );
			}
		} // end if o == 0
		
		else if ( o == 1 ) {
			if ( comp != 0 ) val = Math.min( val, safeAdd( isInSigURecursive( 1, i-1, l-1 ), (comp<0?1:0) ) );
			else val = Math.min( val, isInSigURecursive( 0, i-1, l-1 ) );
			for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
				int[] rhs = rule.getRight();
				int num_smaller = 0;
				Boolean isValid = false;
				for (int j=0; j<rhs.length; j++) {
					// check whether the rule generates [target_token, k].
					isValid |= (target == rhs[j]);
					num_smaller += globalOrder.compare( rhs[j], target )<0?1:0;
				}
				if ( i-rule.leftSize() >= 0 && l-rule.rightSize() >= 0) {
					val = Math.min( val, safeAdd( isInSigURecursive( 1, i-rule.leftSize(), l-rule.rightSize() ), num_smaller ) );
					if (isValid) val = Math.min( val, safeAdd( isInSigURecursive( 0, i-rule.leftSize(), l-rule.rightSize() ), num_smaller ) );
				}
			}
		} // end else if o == 1
		
		mem.put( key, val );
		return val;
	}
}
