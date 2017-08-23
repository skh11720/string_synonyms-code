package snu.kdd.synonym.synonymRev.validator;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

public abstract class Validator {
	public long checked;
	public long recursivecalls;
	public long niterentry;
	public long niterrules;
	public long nitermatches;
	public long nitertokens;
	public long earlyevaled;
	public long earlystopped;
	public long filtered;

	public abstract int isEqual( Record x, Record y );

	protected static boolean areSameString( Record s, Record t ) {
		if( s.getTokenCount() != t.getTokenCount() ) {
			return false;
		}

		int[] si = s.getTokensArray();
		int[] ti = t.getTokensArray();
		for( int i = 0; i < si.length; ++i ) {
			if( si[ i ] != ti[ i ] ) {
				return false;
			}
		}

		return true;
	}

	public void addStat( StatContainer stat ) {
		stat.add( "Val_Comparisons", checked );
		stat.add( "Val_Recursive_Calls", recursivecalls );
		stat.add( "Val_Iter_entries", niterentry );
		stat.add( "Val_Iter_rules", niterrules );
		stat.add( "Val_Iter_matches", nitermatches );
		stat.add( "Val_Iter_tokens", nitertokens );
		stat.add( "Val_Early_evaled", earlyevaled );
		stat.add( "Val_Early_stopped", earlystopped );
		stat.add( "Val_Length_filtered", filtered );
	}

	public void printStats() {
		if( DEBUG.ValidateON ) {
			Util.printLog( "Comparisons: " + checked );
			Util.printLog( "Total recursive calls: " + recursivecalls );
			Util.printLog( "Total iter entries: " + niterentry );
			Util.printLog( "Total iter rules: " + niterrules );
			Util.printLog( "Total iter matches: " + nitermatches );
			Util.printLog( "Total iter tokens: " + nitertokens );
			Util.printLog( "Early evaled: " + earlyevaled );
			Util.printLog( "Early stopped: " + earlystopped );
			Util.printLog( "Length filtered: " + filtered );
		}
	}
}
