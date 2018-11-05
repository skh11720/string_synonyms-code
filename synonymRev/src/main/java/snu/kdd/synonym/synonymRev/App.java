package snu.kdd.synonym.synonymRev;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate.AlgorithmName;
import snu.kdd.synonym.synonymRev.algorithm.JoinHybridAll;
import snu.kdd.synonym.synonymRev.algorithm.JoinHybridAll3;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinFast;
import snu.kdd.synonym.synonymRev.algorithm.JoinNaive;
import snu.kdd.synonym.synonymRev.algorithm.JoinSetNaive;
import snu.kdd.synonym.synonymRev.algorithm.PassJoin;
import snu.kdd.synonym.synonymRev.algorithm.SIJoin;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaNaive;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaSimple;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.JoinBKPSet;
import snu.kdd.synonym.synonymRev.data.DataInfo;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import vldb17.seq.JoinPkduck;
import vldb17.set.JoinPkduckSet;

public class App {
	private static Options argOptions;
	
	public static CommandLine parseInput( String args[] ) throws ParseException {
		if( argOptions == null ) {
			Options options = new Options();
			options.addOption( "rulePath", true, "rule path" );
			options.addOption( "dataOnePath", true, "data one path" );
			options.addOption( "dataTwoPath", true, "data two path" );
			options.addOption( "outputPath", true, "output path" );
			options.addOption( "oneSideJoin", true, "One side join" );
			options.addOption( "algorithm", true, "Algorithm" );
			options.addOption( "split", false, "Split datasets" );
			options.addOption( "upload", true, "Upload experiments" );

			options.addOption( "additional", true, "Additional input arguments" );

			argOptions = options;
		}

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args, false );
		return cmd;
	}
	
	public static DataInfo getDataInfo( CommandLine cmd ) {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String dataOnePath = cmd.getOptionValue( "dataOnePath" );
		final String dataTwoPath = cmd.getOptionValue( "dataTwoPath" );
		return new DataInfo( dataOnePath, dataTwoPath, rulePath );
	}
	
	public static Query getQuery( CommandLine cmd ) throws IOException {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String dataOnePath = cmd.getOptionValue( "dataOnePath" );
		final String dataTwoPath = cmd.getOptionValue( "dataTwoPath" );
		final String outputPath = cmd.getOptionValue( "outputPath" );
		Boolean oneSideJoin = Boolean.parseBoolean( cmd.getOptionValue( "oneSideJoin" ) );
		return new Query( rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath );
	}
	
	public static AlgorithmInterface getAlgorithm( Query query, StatContainer stat, CommandLine cmd ) throws IOException {
		AlgorithmInterface alg = null;
		AlgorithmName algorithmName = AlgorithmName.valueOf( cmd.getOptionValue( "algorithm" ) );

		switch( algorithmName ) {
		case JoinNaive:
			alg = new JoinNaive( query, stat );
			break;

		case JoinMH:
			alg = new JoinMH( query, stat );
			break;

		case JoinMin:
			alg = new JoinMin( query, stat );
			break;

		case JoinMinFast:
			alg = new JoinMinFast( query, stat );
			break;

		case JoinHybridAll:
			alg = new JoinHybridAll( query, stat );
			break;

		case JoinHybridAll3:
			alg = new JoinHybridAll3( query, stat );
			break;

		case SIJoin:
			alg = new SIJoin( query, stat );
			break;

		case JoinPkduck:
			alg = new JoinPkduck( query, stat );
			break;

		case JoinPkduckSet:
			alg = new JoinPkduckSet( query, stat );
			break;

		case JoinBKPSet:
			alg = new JoinBKPSet ( query, stat );
			break;

		case JoinSetNaive:
			alg = new JoinSetNaive( query, stat );
			break;

		case PassJoin:
			alg = new PassJoin( query, stat );
			break;
		
		case JoinDeltaNaive:
			alg = new JoinDeltaNaive( query, stat );
			break;

		case JoinDeltaSimple:
			alg = new JoinDeltaSimple( query, stat );
			break;

		
		default:
			Util.printLog( "Invalid algorithm " + algorithmName );
			System.exit( 0 );
			break;
		}
		
		// if query is not a self join, conduct semi-unidirectional join.
		// 18.09.26: disable semi-unidirectional computation
//		if ( !query.selfJoin && !query.oneSideJoin ) alg = new AlgorithmSemiUniWrapper( (AlgorithmTemplate)alg );

		stat.addPrimary( "Date", "\"" + new Date().toString().replaceAll( " ", "_" ) + "\"" );
		stat.add( cmd );
		stat.add( "cmd_alg", alg.getName() );
		stat.add( "cmd_alg_v", alg.getVersion() );
		
		return alg;
	}
	
	public static void run( AlgorithmInterface alg, Query query, CommandLine cmd ) throws IOException, ParseException {
		String additionalOptions = cmd.getOptionValue( "additional", "" );
		if( additionalOptions != null ) {
			String additionalArgs[] = additionalOptions.split( " " );
			alg.run( query, additionalArgs );
		}
		else {
			alg.run( query, null );
		}
	}
	
	public static void main( String args[] ) throws IOException, ParseException {

		CommandLine cmd = parseInput( args );
		Util.printArgsError( cmd );

		StopWatch totalTime = StopWatch.getWatchStarted( "Result_0_Total_Time" );
		StopWatch initializeTime = StopWatch.getWatchStarted( "Result_1_Initialize_Time" );
		
		Query query = getQuery( cmd );
		StatContainer stat = new StatContainer();
		AlgorithmInterface alg = getAlgorithm( query, stat, cmd );

		initializeTime.stopAndAdd( stat );
		run( alg, query, cmd );

		totalTime.stop();
		Util.printGCStats( stat, "Stat" );

		stat.addPrimary( totalTime );
		addWYKMapCount( stat );
		alg.printStat();

		stat.resultWriter( "result/" + alg.getName() + "_" + alg.getVersion() );

//		DataInfo dataInfo = new DataInfo( dataOnePath, dataTwoPath, rulePath );

		boolean upload = Boolean.parseBoolean( cmd.getOptionValue( "upload" ) );
		if( upload ) {
			alg.writeJSON( getDataInfo( cmd ), cmd );
		}

		Util.printLog( alg.getName() + " finished" );

	}

	public static void addWYKMapCount( StatContainer stat ) {
		stat.add( "hm_getCount", WYK_HashMap.getCount );
		stat.add( "hm_getIterCount", WYK_HashMap.getIterCount );
		stat.add( "hm_putCount", WYK_HashMap.putCount );
		stat.add( "hm_resizeCount", WYK_HashMap.resizeCount );
		stat.add( "hm_removeCount", WYK_HashMap.removeCount );
		stat.add( "hm_removeIterCount", WYK_HashMap.removeIterCount );
		stat.add( "hm_putRemovedCount", WYK_HashMap.putRemovedCount );
		stat.add( "hm_removeFoundCount", WYK_HashMap.removeFoundCount );
	}
}
