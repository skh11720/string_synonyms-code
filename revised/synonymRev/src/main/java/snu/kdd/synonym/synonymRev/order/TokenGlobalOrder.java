package snu.kdd.synonym.synonymRev.order;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.synonym.synonymRev.data.Rule;

public class TokenGlobalOrder extends AbstractGlobalOrder<Integer> {
	
	public TokenGlobalOrder( String mode ) {
		super( mode );
	}

	@Override
	protected IntArrayList parseRule( Rule rule, int pos ) {
		int[] rhs = rule.getRight();
		IntArrayList keyList = new IntArrayList();
		for ( int j=0; j<rhs.length; j++ )
			keyList.add( rhs[j] );
		return keyList;
	}

	@Override
	public int compare( Integer o1, Integer o2 ) {
		switch (mode) {
		case TF: return Integer.compare( o1.intValue(), o2.intValue() );
		case FF: return Integer.compare( orderMap.getInt( o1 ), orderMap.getInt( o2 ) );
		default: throw new RuntimeException("Unexpected error");
		}
	}
}
