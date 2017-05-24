package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.Collection;

import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.JoinMinIndex;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import snu.kdd.synonym.tools.Util;
import tools.DEBUG;
import tools.IntegerPair;
import tools.Rule;
import tools.RuleTrie;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinMinTwo_Q extends AlgorithmTemplate {
	public boolean useAutomata = false;
	public boolean skipChecking = false;
	public int maxIndex = Integer.MAX_VALUE;
	public boolean compact = true;
	public int qSize = 0;
	// public boolean singleside = false;
	// public boolean exact2grams = false;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	public static Validator checker;

	/**
	 * Key: (2gram, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	public JoinMinIndex idx;

	public JoinMinTwo_Q( String rulefile, String Rfile, String Sfile, String outputFile, DataInfo dataInfo, boolean joinOneSide )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputFile, dataInfo, joinOneSide );

		Record.setStrList( strlist );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( getRulelist() );
		Record.setRuleTrie( ruletrie );
	}

	public JoinMinTwo_Q( AlgorithmTemplate o, StatContainer stat ) {
		super( o );

		Record.setStrList( strlist );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
		Record.setRuleTrie( ruletrie );

		this.stat = stat;
	}

	private void buildIndex( boolean writeResult ) throws IOException {
		idx = JoinMinIndex.buildIndexTwo( tableSearched, tableIndexed, maxIndex, qSize, stat, writeResult );
	}

	public void statistics() {
		long strlengthsum = 0;
		long strmaxinvsearchrangesum = 0;
		int strs = 0;
		int maxstrlength = 0;

		long rhslengthsum = 0;
		int rules = 0;
		int maxrhslength = 0;

		for( Record rec : tableSearched ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}
		for( Record rec : tableIndexed ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}

		for( Rule rule : getRulelist() ) {
			int length = rule.getTo().length;
			++rules;
			rhslengthsum += length;
			maxrhslength = Math.max( maxrhslength, length );
		}

		Util.printLog( "Average str length: " + strlengthsum + "/" + strs );
		Util.printLog( "Average maxinvsearchrange: " + strmaxinvsearchrangesum + "/" + strs );
		Util.printLog( "Maximum str length: " + maxstrlength );
		Util.printLog( "Average rhs length: " + rhslengthsum + "/" + rules );
		Util.printLog( "Maximum rhs length: " + maxrhslength );
	}

	public void runWithoutPreprocess( boolean writeResult ) {
		// Retrieve statistics
		StopWatch stepTime = null;
		if( DEBUG.JoinMinON ) {
			statistics();
			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}

		try {
			buildIndex( writeResult );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				stepTime.stopAndAdd( stat );
				stat.add( "Mem_3_BuildIndex", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			}
			else {
				stepTime.stop();
			}
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
		}

		Collection<IntegerPair> rslt = idx.joinTwo( tableSearched, writeResult, stat, checker );

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				stepTime.stopAndAdd( stat );
				stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );

				stat.add( "Counter_Final_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Final_1_HashResize", WYK_HashSet.resize );

				stat.add( "Counter_Final_2_MapCollision", WYK_HashMap.collision );
				stat.add( "Counter_Final_2_MapResize", WYK_HashMap.resize );
			}
			else {
				stat.add( "Sample_JoinMin_Result", rslt.size() );
				stepTime.stop();
			}
		}

		if( writeResult ) {
			if( DEBUG.JoinMinON ) {
				stepTime.resetAndStart( "Result_4_Write_Time" );
			}

			this.writeResult( rslt );

			if( DEBUG.JoinMinON ) {
				stepTime.stopAndAdd( stat );
			}
		}
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "JoinMinTwo_Q";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;

		Param params = Param.parseArgs( args, stat );

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		compact = params.isCompact();
		checker = params.getValidator();
		qSize = params.getQGramSize();

		StopWatch preprocessTime = null;
		if( DEBUG.JoinMinON ) {
			preprocessTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		}
		preprocess( compact, maxIndex, useAutomata );

		if( DEBUG.JoinMinON ) {
			preprocessTime.stopAndAdd( stat );

			stat.add( "Mem_2_Preprocessed", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			preprocessTime.resetAndStart( "Result_3_Run_Time" );
		}

		runWithoutPreprocess( true );

		if( DEBUG.JoinMinON ) {
			preprocessTime.stopAndAdd( stat );
			Validator.printStats();
		}
	}

	public double getGamma() {
		return idx.gamma;
	}

	public double getDelta() {
		return idx.delta;
	}

	public double getEpsilon() {
		return idx.epsilon;
	}
}
