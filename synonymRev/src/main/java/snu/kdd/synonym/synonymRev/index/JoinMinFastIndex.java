package snu.kdd.synonym.synonymRev.index;

import java.util.List;
import java.util.Random;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMinFastIndex extends JoinMinIndex {
	
	private double sampleRatio;
	
	protected JoinMinFastIndex( int indexK, int qSize, Query query, double sampleRatio ) {
		super( indexK, qSize, query );
		this.sampleRatio = sampleRatio;
	}

	public JoinMinFastIndex( int indexK, int qSize, StatContainer stat, Query query, double sampleRatio, int threshold, boolean writeResult ) {
		this( indexK, qSize, query, sampleRatio );
		initialize( stat, threshold, writeResult );
	}

	@Override
	protected List<Record> prepareCountInvokes() {
		Random rn = new Random( System.currentTimeMillis() );
		ObjectArrayList<Record> searchedList = new ObjectArrayList<>();
		for( Record r : query.searchedSet.recordList ) {
			if ( r.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			if( rn.nextDouble() < this.sampleRatio ) {
				searchedList.add( r );
			}
		}
		return searchedList;
	}
	
	public void setSampleRatio( double value ) { sampleRatio = value; }
}
