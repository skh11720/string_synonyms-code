package vldb17.set;

import java.util.Arrays;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;

public class PkduckSetDP {
	
	protected final AbstractGlobalOrder globalOrder;
	protected final int len_max_S;
	protected final Record rec;
	protected final int lenPrefix;
	protected final int[] tokens;
	
	protected static final int inf = Integer.MAX_VALUE/2;

	protected static int safeAdd( int a, int b ) {
		if ( a > 0 && b > 0 && a+b < 0 ) // positive overflow detected
			return Integer.MAX_VALUE;
		else return a+b;
	}
	
	public PkduckSetDP( Record rec, double theta, AbstractGlobalOrder globalOrder ) {
		this.rec = rec;
		this.lenPrefix = (int)Math.floor((1-theta)*rec.size())+1;
		this.tokens = rec.getTokensArray();
		this.len_max_S = rec.getMaxTransLength();
		this.globalOrder = globalOrder;
	}

	public Boolean isInSigU( int target ) {
		/*
		 * Compute g[o][i][l] for o=0,1, i=0~|rec|, l=0~max(|recS|).
		 * g[1][i][l] is X_l in the MIT paper.
		 */
//		System.out.println( "PkduckIndex.isInSigU, "+target_qgram+", "+k );
		
		// initialize g.
		int[][][] g = new int[2][rec.size()+1][len_max_S+1];
		for (int o=0; o<2; o++) {
			for (int i=0; i<=rec.size(); i++ ) {
				Arrays.fill( g[o][i], inf ); // divide by 2 to prevent overflow
			}
		}
		g[0][0][0] = 0;

		// compute g[0][i][l].
		for (int i=1; i<=rec.size(); i++) {
			int token = tokens[i-1];
			for (int l=1; l<=len_max_S; l++) {
				int comp = globalOrder.compare( token, target );
//				System.out.println( "comp: "+comp );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				if ( comp != 0 ) g[0][i][l] = Math.min( g[0][i][l], safeAdd(g[0][i-1][l-1], (comp<0?1:0) ) );
//				System.out.println( "g[0]["+(i-1)+"]["+(l-1)+"]: "+g[0][i-1][l-1] );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
//					System.out.println( rule );
					int[] rhs = rule.getRight();
					int num_smaller = 0;
					Boolean isValid = true;
					for (int j=0; j<rhs.length; j++) {
						// check whether the rule does not generate [target_token, k].
						isValid &= (target != rhs[j]);
						num_smaller += globalOrder.compare( rhs[j], target )<0?1:0;
					}
//					System.out.println( "isValid: "+isValid );
//					System.out.println( "num_smaller: "+num_smaller );
					if (isValid && i-rule.leftSize() >= 0 && l-rule.rightSize() >= 0) 
						g[0][i][l] = Math.min( g[0][i][l], safeAdd(g[0][i-rule.leftSize()][l-rule.rightSize()], num_smaller) );
				}
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		
		// compute g[1][i][l].
		for (int i=1; i<=rec.size(); i++ ) {
			int token = tokens[i-1];
			for (int l=1; l<=len_max_S; l++) {
				int comp = globalOrder.compare( token, target );
//				System.out.println( "comp: "+comp );
				if ( comp != 0 ) g[1][i][l] = Math.min( g[1][i][l], safeAdd(g[1][i-1][l-1], (comp<0?1:0)) );
				else g[1][i][l] = Math.min( g[1][i][l], g[0][i-1][l-1] );
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
				for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
//					System.out.println( rule );
					int[] rhs = rule.getRight();
					int num_smaller = 0;
					Boolean isValid = false;
					for (int j=0; j<rhs.length; j++) {
						// check whether the rule generates [target_token, k].
						isValid |= (target == rhs[j]);
						num_smaller += globalOrder.compare( rhs[j], target )<0?1:0;
					}
//					System.out.println( "isValid: "+isValid );
//					System.out.println( "num_smaller: "+num_smaller );
					if ( i-rule.leftSize() >= 0 && l-rule.rightSize() >= 0) {
						g[1][i][l] = Math.min( g[1][i][l], safeAdd( g[1][i-rule.leftSize()][l-rule.rightSize()], num_smaller ) );
						if (isValid) g[1][i][l] = Math.min( g[1][i][l], safeAdd( g[0][i-rule.leftSize()][l-rule.rightSize()], num_smaller ) );
					}
				}
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));

		Boolean res = false;
		for (int l=1; l<=len_max_S; l++) res |= (g[1][rec.size()][l] <= lenPrefix-1);
		return res;
	}
}
