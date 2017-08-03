package snu.kdd.synonym.synonymRev.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.DataInfo;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;

public abstract class AlgorithmTemplate {
	public enum AlgorithmName {
		JoinNaive, JoinMH, JoinBK, JoinMin, JoinMinPosition, JoinMinRange, JoinMinNaive, JoinMinNaiveThres, JoinMHNaive
	}

	// contains statistics of the algorithm
	StatContainer stat;
	protected Query query;
	protected ACAutomataR automata;

	public AlgorithmTemplate( Query query, StatContainer stat ) throws IOException {
		this.stat = stat;
		this.query = query;
	}

	public abstract String getName();

	public abstract String getVersion();

	public abstract void run( Query query, String[] args ) throws IOException, ParseException;

	public void printStat() {
		System.out.println( "=============[" + this.getName() + " stats" + "]=============" );
		stat.printResult();
		System.out.println(
				"==============" + new String( new char[ getName().length() ] ).replace( "\0", "=" ) + "====================" );
	}

	protected void preprocess() {
		// builds an automata of the set of rules
		StopWatch preprocessTime = null;

		stat.addMemory( "Mem_1_Initialized" );

		if( DEBUG.AlgorithmON ) {
			preprocessTime = StopWatch.getWatchStarted( "Result_2_1_Preprocess rule time" );
		}

		automata = new ACAutomataR( query.ruleSet.get() );

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

	public void writeResult( Collection<IntegerPair> rslt ) {
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

				bw.write( r.toString( query.tokenIndex ) + "(" + r.getID() + ")\t==\t" + s.toString( query.tokenIndex ) + "("
						+ s.getID() + ")\n" );
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

			bw_json.write( "{" );

			bw_json.write( "\"Date\": \"" + new Date().toString() + "\"," );

			bw_json.write( "\"Algorithm\": {" );
			bw_json.write( "\"name\": \"" + getName() + "\"," );
			bw_json.write( "\"version\": \"" + getVersion() + "\"" );
			bw_json.write( "}" );

			bw_json.write( ", \"Result\":{" );
			bw_json.write( stat.toJson() );
			bw_json.write( "}" );

			bw_json.write( ", \"Dataset\": {" );
			bw_json.write( dataInfo.toJson() );
			bw_json.write( "}" );

			bw_json.write( ", \"ParametersUsed\": {" );
			bw_json.write( "\"additional\": " );
			bw_json.write( "\"" + cmd.getOptionValue( "additional", "" ) + "\"," );
			bw_json.write( "\"oneSideJoin\": " );
			bw_json.write( "\"" + cmd.getOptionValue( "oneSideJoin" ) + "\"" );
			bw_json.write( "}" );

			bw_json.write( "}\n" );
			bw_json.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}
}
