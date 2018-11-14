package snu.kdd.synonym.synonymRev.algorithm.set;

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;

public class SetNaiveOneSide extends AbstractSetValidator {
	
	public SetNaiveOneSide( Boolean selfJoin ) {
		super( selfJoin );
	}

	@Override
	protected int isEqualOneSide( Record x, Record y ) {
		IntOpenHashSet ySet = new IntOpenHashSet(y.getTokensArray());
		List<Record> expandedX = x.expandAll();
		for( Record xPrime : expandedX ) {
			IntOpenHashSet xPrimeSet = new IntOpenHashSet(xPrime.getTokensArray());
			if ( ySet.equals( xPrimeSet ) ) {
				return 1;
			}
		}
		return -1;
	}

	@Override
	public String getName() {
		return "SetNaiveOneSideValidator";
	}
}
