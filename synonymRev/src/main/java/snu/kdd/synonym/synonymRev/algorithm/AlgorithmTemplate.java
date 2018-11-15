package snu.kdd.synonym.synonymRev.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.DataInfo;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.AbstractParam;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;

public abstract class AlgorithmTemplate implements AlgorithmInterface {
	public enum AlgorithmName {
		JoinNaive,
		JoinMH,
		JoinMin,
		JoinMinFast,
		JoinHybridAll,
		JoinHybridAll3,
		SIJoin,
		JoinPkduck,

		JoinSetNaive,
		JoinPkduckSet,
		JoinBKPSet,
		PassJoin,
		
		JoinDeltaNaive,
		JoinDeltaSimple,
		JoinDeltaVar,
	}

	// contains statistics of the algorithm
	public boolean writeResult = true;
	protected final StatContainer stat;
	protected final Query query;
	protected AbstractParam param;
	public Collection<IntegerPair> rslt = null;


	public AlgorithmTemplate( Query query, StatContainer stat, String[] args ) throws IOException, ParseException {
		this.stat = stat;
		this.query = query;
	}

	public abstract String getName();

	public abstract String getVersion();

	public abstract void run();

	protected void preprocess() {
		// builds an automata of the set of rules
		StopWatch preprocessTime = null;

		stat.addMemory( "Mem_1_Initialized" );

		if( DEBUG.AlgorithmON ) {
			preprocessTime = StopWatch.getWatchStarted( "Result_2_1_Preprocess rule time" );
		}

		ACAutomataR automata = new ACAutomataR( query.ruleSet.get() );

		long applicableRules = 0;

		// Preprocess each records in R
		for( final Record rec : query.searchedSet.get() ) {
			rec.preprocessRules( automata );

			if( DEBUG.AlgorithmON ) {
				applicableRules += rec.getNumApplicableRules();
			}
		}

		if( DEBUG.AlgorithmON ) {
			preprocessTime.stopQuietAndAdd( stat );
			stat.add( "Stat_Applicable Rule TableSearched", applicableRules );
			stat.add( "Stat_Avg applicable rules", Double.toString( (double) applicableRules / query.searchedSet.size() ) );

			preprocessTime.resetAndStart( "Result_2_2_Preprocess length time" );
		}

		for( final Record rec : query.searchedSet.get() ) {
			rec.preprocessTransformLength();
		}

		if( DEBUG.AlgorithmON ) {
			preprocessTime.stopQuietAndAdd( stat );

			preprocessTime.resetAndStart( "Result_2_3_Preprocess est record time" );
		}

		long maxTSize = 0;
		for( final Record rec : query.searchedSet.get() ) {
			rec.preprocessEstimatedRecords();

			if( DEBUG.AlgorithmON ) {
				long est = rec.getEstNumTransformed();

				if( maxTSize < est ) {
					maxTSize = est;
				}
			}
		}

		if( DEBUG.AlgorithmON ) {
			stat.add( "Stat_maximum Size of Table T", maxTSize );

			preprocessTime.stopQuietAndAdd( stat );
			// Preprocess each records in S
			preprocessTime.resetAndStart( "Result_2_6_Preprocess records in S time" );
		}

		long maxSSize = 0;
		applicableRules = 0;

		if( !query.selfJoin ) {
			for( final Record rec : query.indexedSet.get() ) {
				rec.preprocessRules( automata );
				rec.preprocessTransformLength();
				rec.preprocessEstimatedRecords();

				if( DEBUG.AlgorithmON ) {
					applicableRules += rec.getNumApplicableRules();

					long est = rec.getEstNumTransformed();
					if( maxSSize < est ) {
						maxSSize = est;
					}
				}
			}
		}

		if( DEBUG.AlgorithmON ) {
			stat.add( "Stat_maximum Size of Table S", maxSSize );
			stat.add( "Stat_Applicable Rule TableIndexed", applicableRules );

			preprocessTime.stopQuietAndAdd( stat );
		}
	}

	public void printStat() {
		System.out.println( "=============[" + this.getName() + " stats" + "]=============" );
		stat.printResult();
		System.out.println(
				"==============" + new String( new char[ getName().length() ] ).replace( "\0", "=" ) + "====================" );
	}

	public void writeResult() {
		if ( !writeResult ) return;
		stat.addPrimary( "Final Result Size", rslt.size() );

		try {
			if( DEBUG.AlgorithmON ) {
				Util.printLog( "Writing results " + rslt.size() );
			}

			final BufferedWriter bw = new BufferedWriter( new FileWriter( query.outputFile + "/" + getName() ) );
			bw.write( rslt.size() + "\n" );
			for( final IntegerPair ip : rslt ) {
				final Record r = query.searchedSet.getRecord( ip.i1 );
				final Record s = query.indexedSet.getRecord( ip.i2 );

				if( !DEBUG.printSelfJoinON ) {
					if( query.selfJoin && r.equals( s ) ) {
						continue;
					}
				}

//				bw.write( r.toString( query.tokenIndex ) + "(" + r.getID() + ")\t==\t" + s.toString( query.tokenIndex ) + "("+ s.getID() + ")\n" );
//				bw.write( "(" + r.getID() + ")\t==\t" + "("+ s.getID() + ")\n" );
				bw.write( Arrays.toString( r.getTokensArray() ) + "(" + r.getID() + ")\t==\t" + Arrays.toString( s.getTokensArray() ) + "("+ s.getID() + ")\n" );
			}
			bw.close();
		}
		catch( final Exception e ) {
			e.printStackTrace();
			Util.printLog( "Error: " + e.getMessage() );
		}
	}

	public void writeJSON( DataInfo dataInfo, CommandLine cmd ) {
		BufferedWriter bw_json;
		try {
			bw_json = new BufferedWriter( new FileWriter(
					"json/" + this.getName() + "_"
							+ new java.text.SimpleDateFormat( "yyyyMMdd_HHmmss_z" ).format( new java.util.Date() ) + ".txt",
					true ) );

			// start JSON object
			bw_json.write( "{" );
			// metadata
			bw_json.write( "\"Date\":\"" + new Date().toString() + "\", " );
			// input
			bw_json.write("\"Input\":{");
				// input.algorithm
				bw_json.write( "\"Algorithm\":{" );
					bw_json.write( "\"Name\":\"" + getName() + "\", " );
					bw_json.write( "\"Version\":\"" + getVersion() + "\", " );
					// input.algorithm.param
					bw_json.write("\"Param\":"+param.getJSONString() );
				bw_json.write( "}" );
				// input.dataset
				bw_json.write( ", \"Dataset\":{" );
				bw_json.write( dataInfo.toJson() );
				bw_json.write( "}" );
			bw_json.write( "}" );
			// output
			bw_json.write( ", \"Output\":{" );
			bw_json.write( stat.toJson() );
			bw_json.write( "}" );

			bw_json.write( "}\n" );
			bw_json.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	@Override
	public Collection<IntegerPair> getResult() {
		return rslt;
	}
	
	public static void addSeqResult( Record rec1, Record rec2, Set<IntegerPair> rslt, boolean isSelfJoin ) {
		if ( isSelfJoin ) {
			int id_smaller = rec1.getID() < rec2.getID()? rec1.getID() : rec2.getID();
			int id_larger = rec1.getID() >= rec2.getID()? rec1.getID() : rec2.getID();
			rslt.add( new IntegerPair( id_smaller, id_larger) );
		}
		else rslt.add( new IntegerPair(rec1.getID(), rec2.getID()) );
	}

	public static void addSeqResult( Record rec1, int rec2id, Set<IntegerPair> rslt, boolean isSelfJoin ) {
		if ( isSelfJoin ) {
			int id_smaller = rec1.getID() < rec2id? rec1.getID() : rec2id;
			int id_larger = rec1.getID() >= rec2id? rec1.getID() : rec2id;
			rslt.add( new IntegerPair( id_smaller, id_larger) );
		}
		else rslt.add( new IntegerPair(rec1.getID(), rec2id) );
	}
	
	public static void addSetResult( Record rec1, Record rec2, Set<IntegerPair> rslt, boolean leftFromS, boolean isSelfJoin ) {
		if ( isSelfJoin ) {
			int id_smaller = rec1.getID() < rec2.getID()? rec1.getID() : rec2.getID();
			int id_larger = rec1.getID() >= rec2.getID()? rec1.getID() : rec2.getID();
			rslt.add( new IntegerPair( id_smaller, id_larger) );
		}
		else {
			// idx == idxT
			if ( leftFromS ) rslt.add( new IntegerPair( rec1.getID(), rec2.getID()) );
			// idx == idxS
			else rslt.add( new IntegerPair( rec2.getID(), rec1.getID()) );
		}
	}
	
	@Override
	public void setWriteResult( boolean flag ) {
		this.writeResult = flag;
	}
	
	@Override
	public StatContainer getStat() { return stat; }
}
