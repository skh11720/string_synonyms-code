package snu.kdd.synonym.synonymRev;

import java.io.IOException;

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
import snu.kdd.synonym.synonymRev.algorithm.SIJoinOriginal;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaNaive;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaSimple;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVar;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVarBK;
import snu.kdd.synonym.synonymRev.algorithm.set.JoinBKPSet;
import snu.kdd.synonym.synonymRev.data.DataInfo;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import vldb17.seq.JoinPkduck;
import vldb17.set.JoinPkduckOriginal;
import vldb17.set.JoinPkduckSet;

public class App {
	private static Options argOptions;
	
	public static void main( String args[] ) throws IOException, ParseException {

		CommandLine cmd = parseInput( args );
		Util.printArgsError( cmd );

		StopWatch totalTime = StopWatch.getWatchStarted( "Result_0_Total_Time" );
		StopWatch initializeTime = StopWatch.getWatchStarted( "Result_1_Initialize_Time" );
		
		Query query = Query.parseQuery( cmd );
		StatContainer stat = new StatContainer();
		AlgorithmInterface alg = getAlgorithm( query, cmd );

		initializeTime.stopAndAdd( stat );
		alg.run();

		totalTime.stop();
		Util.printGCStats( stat, "Stat" );

		stat.addPrimary( totalTime );
		alg.printStat();

		stat.resultWriter( "result/" + alg.getName() + "_" + alg.getVersion() );

		boolean upload = Boolean.parseBoolean( cmd.getOptionValue( "upload" ) );
		if( upload ) {
			alg.writeJSON();
		}

		Util.printLog( alg.getName() + " finished" );
	}
	
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
	
	public static AlgorithmInterface getAlgorithm( Query query, CommandLine cmd ) throws IOException, ParseException {
		AlgorithmInterface alg = null;
		AlgorithmName algorithmName = AlgorithmName.valueOf( cmd.getOptionValue( "algorithm" ) );
		String additionalOptions = cmd.getOptionValue( "additional", "" );
		String[] additionalArgs = null;
		if( additionalOptions != null ) additionalArgs = additionalOptions.split( " " );

		switch( algorithmName ) {
		case JoinNaive:
			alg = new JoinNaive( query, additionalArgs );
			break;

		case JoinMH:
			alg = new JoinMH( query, additionalArgs );
			break;

		case JoinMin:
			alg = new JoinMin( query, additionalArgs );
			break;

		case JoinMinFast:
			alg = new JoinMinFast( query, additionalArgs );
			break;

		case JoinHybridAll:
			alg = new JoinHybridAll( query, additionalArgs );
			break;

		case JoinHybridAll3:
			alg = new JoinHybridAll3( query, additionalArgs );
			break;

		case SIJoin:
			alg = new SIJoin( query, additionalArgs );
			break;

		case SIJoinOriginal:
			alg = new SIJoinOriginal( query, additionalArgs );
			break;

		case JoinPkduck:
			alg = new JoinPkduck( query, additionalArgs );
			break;

		case JoinPkduckSet:
			alg = new JoinPkduckSet( query, additionalArgs );
			break;

		case JoinPkduckOriginal:
			alg = new JoinPkduckOriginal( query, additionalArgs );
			break;

		case JoinBKPSet:
			alg = new JoinBKPSet ( query, additionalArgs );
			break;

		case JoinSetNaive:
			alg = new JoinSetNaive( query, additionalArgs );
			break;

		case PassJoin:
			alg = new PassJoin( query, additionalArgs );
			break;
		
		case JoinDeltaNaive:
			alg = new JoinDeltaNaive( query, additionalArgs );
			break;

		case JoinDeltaSimple:
			alg = new JoinDeltaSimple( query, additionalArgs );
			break;

		case JoinDeltaVar:
			alg = new JoinDeltaVar( query, additionalArgs );
			break;

		case JoinDeltaVarBK:
			alg = new JoinDeltaVarBK( query, additionalArgs );
			break;

		
		default:
			Util.printLog( "Invalid algorithm " + algorithmName );
			System.exit( 0 );
			break;
		}
		
		return alg;
	}
}
