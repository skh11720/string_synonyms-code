package snu.kdd.synonym.synonymRev.index;

import java.util.Set;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.Validator;

public abstract class AbstractIndex {
	
	protected int skipped = 0;
	
	protected long joinTime = 0;

	public ResultSet join( Query query, StatContainer stat, Validator checker, boolean writeResult ) {
		final ResultSet rslt = new ResultSet(query.selfJoin);
		final long ts = System.nanoTime();

		for( Record recS : query.searchedSet.recordList ) {
//			if ( !isTargetRecord(recS) ) continue;
			if( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				++skipped;
				continue;
			}
			
			joinOneRecord( recS, rslt, checker );
		}
		joinTime = System.nanoTime() - ts;
		
		stat.add( "Stat_Skipped", skipped );
		postprocessAfterJoin(stat);
		
		return rslt;
	}
	
//	protected abstract boolean isTargetRecord( Record rec );
	
	protected abstract void joinOneRecord( Record recS, ResultSet rslt, Validator checker );
	
	protected abstract void postprocessAfterJoin( StatContainer stat );
}
