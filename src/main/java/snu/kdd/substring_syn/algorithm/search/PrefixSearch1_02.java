package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator3;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.Stat;

public class PrefixSearch1_02 extends PrefixSearch {
	
	/*
	 * Use length filtering in the text-side transformation.
	 * Use TransSetBoundCalculator3. 
	 */

	protected TransSetBoundCalculator3 boundCalculator;

	public PrefixSearch1_02( double theta ) {
		super(theta);
		USE_LF_TEXT_SIDE = true;
	}

	@Override
	protected void setBoundCalculator(Record rec, double modifiedTheta) {
		statContainer.startWatch("Time_TransSetBoundCalculatorMem");
		if ( USE_LF_TEXT_SIDE ) boundCalculator = new TransSetBoundCalculator3(statContainer, rec, modifiedTheta);
		statContainer.stopWatch("Time_TransSetBoundCalculatorMem");
	}

	@Override
	protected boolean applyPrefixFilteringFrom( Record query, Record rec, int widx ) {
		for ( int w=1; w<=rec.size()-widx; ++w ) {
			log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, ...)  widx=%d/%d  w=%d/%d", query.getID(), rec.getID(), widx, rec.size()-1, w, rec.size() );
			if ( USE_LF_TEXT_SIDE ) {
				LFOutput lfOutput = applyLengthFiltering(query, widx, w);
				if ( lfOutput == LFOutput.filtered_ignore ) continue;
				else if ( lfOutput == LFOutput.filtered_stop ) break;
			}
			statContainer.addCount(Stat.Num_TS_WindowSizeLF, w);
			if ( applyPrefixFilteringToWindow(query, rec, widx, w) ) return true;
		}
		return false;
	}

	protected LFOutput applyLengthFiltering( Record query, int widx, int w ) {
		int ub = boundCalculator.getLFUB(widx, widx+w-1);
		int lb = boundCalculator.getLFLB(widx, widx+w-1);
		if ( query.getDistinctTokenCount() > ub ) return LFOutput.filtered_ignore;
		if ( query.getDistinctTokenCount() < lb ) return LFOutput.filtered_ignore;
		else return LFOutput.not_filtered;
	}

	@Override
	public String getVersion() {
		return "1.02";
	}
}
