package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinCatesian extends AlgorithmTemplate {

	// staticitics used for building indexes
	double avgTransformed;
	static Validator checker;
	boolean noLength = false;

	public JoinCatesian( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected void preprocess() {
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

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		// Setup parameters
		System.out.println( "Args " + Arrays.toString( args ) );
		Param params = Param.parseArgs( args, stat, query );
		checker = params.validator;

		noLength = params.noLength;
		System.out.println( "No length " + noLength );

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		final Set<IntegerPair> list = runAfterPreprocess();

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( list );

		stepTime.stopAndAdd( stat );
	}

	public Set<IntegerPair> runAfterPreprocess() {
		Set<IntegerPair> rslt = new ObjectOpenHashSet<>();
		long lengthFiltered = 0;
		for( Record indexedR : query.indexedSet.get() ) {
			for( Record searchedS : query.searchedSet.get() ) {
				if( !noLength ) {
					if( query.oneSideJoin ) {
						if( !StaticFunctions.overlap( indexedR.getTokenCount(), indexedR.getTokenCount(),
								searchedS.getMinTransLength(), searchedS.getMaxTransLength() ) ) {
							lengthFiltered++;
							continue;
						}
					}
					else {
						if( !StaticFunctions.overlap( indexedR.getMinTransLength(), indexedR.getMaxTransLength(),
								searchedS.getMinTransLength(), searchedS.getMaxTransLength() ) ) {
							lengthFiltered++;
							continue;
						}
					}
				}

				if( checker.isEqual( searchedS, indexedR ) >= 0 ) {
//					rslt.add( new IntegerPair( searchedS.getID(), indexedR.getID() ) );
					addSeqResult( searchedS, indexedR, rslt, query.selfJoin );
				}
			}
		}
		stat.add( "Stat_Length_Filtered", lengthFiltered );
		return rslt;
	}

	@Override
	public String getName() {
		return "JoinCatesian";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
