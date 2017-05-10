package validator;

import mine.Record;
import snu.kdd.synonym.tools.Util;
import tools.DEBUG;

public abstract class Validator {
	public static long checked;
	public static long recursivecalls;
	public static long niterentry;
	public static long niterrules;
	public static long nitermatches;
	public static long nitertokens;
	public static long earlyevaled;
	public static long earlystopped;
	public static long filtered;

	public abstract int isEqual( Record x, Record y );

	protected static boolean areSameString( Record s, Record t ) {
		if( s.size() != t.size() )
			return false;
		int[] si = s.getTokenArray();
		int[] ti = t.getTokenArray();
		for( int i = 0; i < s.size(); ++i )
			if( si[ i ] != ti[ i ] )
				return false;
		return true;
	}

	public static void printStats() {
		if( DEBUG.ValidateON ) {
			Util.printLog( "Comparisons: " + Validator.checked );
			Util.printLog( "Total recursive calls: " + Validator.recursivecalls );
			Util.printLog( "Total iter entries: " + Validator.niterentry );
			Util.printLog( "Total iter rules: " + Validator.niterrules );
			Util.printLog( "Total iter matches: " + Validator.nitermatches );
			Util.printLog( "Total iter tokens: " + Validator.nitertokens );
			Util.printLog( "Early evaled: " + Validator.earlyevaled );
			Util.printLog( "Early stopped: " + Validator.earlystopped );
			Util.printLog( "Length filtered: " + Validator.filtered );
		}
	}
}
