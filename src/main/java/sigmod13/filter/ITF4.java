package sigmod13.filter;

import java.util.HashSet;
import java.util.List;

import sigmod13.SIRecord;
import sigmod13.SIRecordExpanded;
import tools.IntegerMap;
import tools.Rule;

public class ITF4 extends ITF_Filter {
	private IntegerMap<Long> tfs;

	public ITF4( List<SIRecord> tableR, List<Rule> rulelist ) {
		super( tableR, rulelist );
		tfs = new IntegerMap<Long>();

		// Generate all the possible expanded sets
		for( SIRecord rec : tableR ) {
			HashSet<SIRecordExpanded> expanded = rec.generateAll();
			// Add tf for original tokens
			for( int token : rec.getTokens() ) {
				Long freq = tfs.get( token );
				if( freq == null )
					freq = (long) expanded.size();
				else
					freq = freq + (long) expanded.size();
				tfs.put( token, freq );
			}
			// Add tf for expanded tokens
			for( SIRecordExpanded exp : expanded ) {
				for( int token : exp.getExpandedTokens() ) {
					Long freq = tfs.get( token );
					if( freq == null )
						freq = 1L;
					else
						freq = freq + 1;
					tfs.put( token, freq );
				}
			}
		}
	}

	@Override
	/* Precedence priority: 1) Less ITF value (the ITF value is integrated,
	 * generate all the strings) */
	public int compare( int t1, boolean t1_from_record, int t2, boolean t2_from_record ) {
		Long f1 = tfs.get( t1 );
		Long f2 = tfs.get( t2 );
		// null equals to 0
		if( f1 == null ) {
			if( f2 == null )
				return 0;
			return -1;
		}
		else if( f2 == null )
			return 1;
		return Long.compare( f1, f2 );
	}
}
