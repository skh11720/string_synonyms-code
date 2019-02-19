package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMin extends AbstractIndexBasedAlgorithm {

	public int qSize;
	public int indexK;

	public Validator checker;

	/**
	 * Key: (2gram, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	public JoinMinIndex idx;

	protected boolean useLF, usePQF, useSTPQ;

	
	public JoinMin(Query query, String[] args) throws IOException, ParseException {
		super(query, args);
		param = new Param(args);
		checker = new TopDownOneSide();
		indexK = param.getIntParam("indexK");
		qSize = param.getIntParam("qSize");
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
		useSTPQ = param.getBooleanParam("useSTPQ");
	}
	
	@Override
	protected void executeJoin() {
		if ( usePQF ) runAfterPreprocess();
		else runAfterPreprocessWithoutIndex();
		checker.addStat( stat );
	}

	public void runAfterPreprocess() {
		// Retrieve statistics
		StopWatch stepTime = null;
		statistics();
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );

		buildIndex( writeResult );

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		rslt = idx.join( query, stat, checker, writeResult );

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
		idx.addStat( stat );
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
				if ( !useLF || StaticFunctions.overlap(range[0], range[1], recT.size(), recT.size() )) {
					candidates.add(recT);
				}
				else ++checker.lengthFiltered;
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
	}

	@Override
	protected void buildIndex( boolean writeResult ) {
		idx = new JoinMinIndex( indexK, qSize, stat, query, 0, writeResult );
		JoinMinIndex.useLF = useLF;
		JoinMinIndex.usePQF = usePQF;
		JoinMinIndex.useSTPQ = useSTPQ;
	}

	public void statistics() {
		// TODO: extract this function from this class
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
