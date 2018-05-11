package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import snu.kdd.synonym.synonymRev.data.Record;

public class PosQGramFilterDPIncTopDown extends PosQGramFilterDPTopDown {
	
	// bTransLen[i][l] indicates that s[1,i] can be transformed to a string of length l.
	
	public PosQGramFilterDPIncTopDown(final Record record, final int q) {
		super(record, q);
		qgram = new int[q];
	}
	
	public final Boolean existence(final int token, final int d, final int k) {
		/*
		 * Compute the existence of qgramPrefix[:d-1] + [token]. 
		 * 
		 * d ranges from 1 to qgramSize.
		 * k ranges from 0.
		 */
		
		// trivial case
		// Note that k starts from 0.
		if (record.getMaxTransLength() <= k ) return false;
		this.k = k;

		// bottom-up recursion at depth d.
		qgram[d-1] = token;
		for ( int i=1; i<=record.size(); i++ ) {
			bGen.removeBoolean( new IntTriple( i, d, 0) );
			bGen.removeBoolean( new IntTriple( i, d, 1) );
		}

		return existenceRecursive( record.size(), d, 1 );
	}
}