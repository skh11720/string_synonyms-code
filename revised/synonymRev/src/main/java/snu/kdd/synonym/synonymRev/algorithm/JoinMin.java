package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndexInterface;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMin extends AlgorithmTemplate {
	public int qSize = 0;
	public int indexK = 0;

	public Validator checker;

	/**
	 * Key: (2gram, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	public JoinMinIndexInterface idx;

	protected boolean useLF, usePQF, useSTPQ;

	public JoinMin( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	protected void setup( Param params ) {
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
		useLF = params.useLF;
		usePQF = params.usePQF;
		useSTPQ = params.useSTPQ;
	}

	@Override
	public void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
			}
		}
	}

	protected void buildIndex( boolean writeResult ) throws IOException {
		idx = new JoinMinIndex( indexK, qSize, stat, query, 0, writeResult );
		JoinMinIndex.useLF = useLF;
		JoinMinIndex.usePQF = usePQF;
		JoinMinIndex.useSTPQ = useSTPQ;
	}

	public void statistics() {
		long strlengthsum = 0;

		int strs = 0;
		int maxstrlength = 0;

		long rhslengthsum = 0;
		int rules = 0;
		int maxrhslength = 0;

		for( Record rec : query.searchedSet.get() ) {
			int length = rec.getTokenCount();
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}

		for( Record rec : query.indexedSet.get() ) {
			int length = rec.getTokenCount();
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}

		for( Rule rule : query.ruleSet.get() ) {
			int length = rule.getRight().length;
			++rules;
			rhslengthsum += length;
			maxrhslength = Math.max( maxrhslength, length );
		}

		Util.printLog( "Average str length: " + strlengthsum + "/" + strs );
		Util.printLog( "Maximum str length: " + maxstrlength );
		Util.printLog( "Average rhs length: " + rhslengthsum + "/" + rules );
		Util.printLog( "Maximum rhs length: " + maxrhslength );
	}

	public void runWithoutPreprocess() throws IOException {
		// Retrieve statistics
		StopWatch stepTime = null;
		statistics();
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );

		buildIndex( writeResult );

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		rslt = idx.joinMaxK( indexK, writeResult, stat, checker, query );

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				stat.add( "Counter_Final_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Final_1_HashResize", WYK_HashSet.resize );

				stat.add( "Counter_Final_2_MapCollision", WYK_HashMap.collision );
				stat.add( "Counter_Final_2_MapResize", WYK_HashMap.resize );
			}
			else {
				stat.add( "Sample_JoinMin_Result", rslt.size() );
			}
		}

		stepTime.resetAndStart( "Result_4_Write_Time" );
		this.writeResult();
		stepTime.stopAndAdd( stat );
	}

	public void runAfterPreprocessWithoutIndex() {
		rslt = new ObjectOpenHashSet<IntegerPair>();
		StopWatch runTime = null;
		//StopWatch stepTime = null;

		runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );
		//stepTime = StopWatch.getWatchStarted( "Result_3_1_Filter_Time" );
		long t_filter = 0;
		long t_verify = 0;

		for ( Record recS : query.searchedSet.recordList ) {
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			long ts = System.nanoTime();
			int[] range = recS.getTransLengths();
			ObjectOpenHashSet<Record> candidates = new ObjectOpenHashSet<>();
			for ( Record recT : query.indexedSet.recordList ) {
				int[] otherRange = recT.getTransLengths();
				if ( !useLF || StaticFunctions.overlap(range[0], range[1], otherRange[0], otherRange[1])) {
					candidates.add(recT);
				}
			}
			
			long afterFilterTime = System.nanoTime();
			for ( Record recT : candidates ) {
				int compare = checker.isEqual(recS, recT);
				if (compare >= 0) {
					addSeqResult( recS, recT, (Set<IntegerPair>) rslt, query.selfJoin );
				}
			}
			long afterVerifyTime = System.nanoTime();
			t_filter += afterFilterTime - ts;
			t_verify += afterVerifyTime - afterFilterTime;
		}

		stat.add( "Result_5_1_Filter_Time", t_filter/1e6 );
		stat.add( "Result_5_2_Verify_Time", t_verify/1e6 );
		
		runTime.stopAndAdd( stat );
		this.writeResult();
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );
		setup( params );

		StopWatch preprocessTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		preprocessTime.stopAndAdd( stat );

		stat.addMemory( "Mem_2_Preprocessed" );
		preprocessTime.resetAndStart( "Result_3_Run_Time" );

		if ( usePQF ) {
			runWithoutPreprocess();
			idx.addStat( stat );
		}
		else runAfterPreprocessWithoutIndex();

		preprocessTime.stopAndAdd( stat );
		checker.addStat( stat );
	}

	public double getLambda() {
		return idx.getLambda();
	}

	public double getMu() {
		return idx.getMu();
	}

	public double getRho() {
		return idx.getRho();
	}

	public long getSearchedTotalSigCount() { return idx.getSearchedTotalSigCount(); }
	public long getIndexedTotalSigCount() { return idx.getIndexedTotalSigCount(); }

	@Override
	public String getVersion() {
		/*
		 * 2.5: the latest version by yjpark
		 * 2.51: checkpoint
		 * 2.511: test for filtering power test
		 */
		return "2.511";
	}

	@Override
	public String getName() {
		return "JoinMin";
	}
}
