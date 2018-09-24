package snu.kdd.synonym.synonymRev;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmSemiUniWrapper;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate.AlgorithmName;
import snu.kdd.synonym.synonymRev.algorithm.JoinBK;
import snu.kdd.synonym.synonymRev.algorithm.JoinBK_Split;
import snu.kdd.synonym.synonymRev.algorithm.JoinCatesian;
import snu.kdd.synonym.synonymRev.algorithm.JoinHybridAll;
import snu.kdd.synonym.synonymRev.algorithm.JoinHybridAll2;
import snu.kdd.synonym.synonymRev.algorithm.JoinHybridAll3;
import snu.kdd.synonym.synonymRev.algorithm.JoinHybridAll_NEW;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.algorithm.JoinMHDP;
import snu.kdd.synonym.synonymRev.algorithm.JoinMHNaive;
import snu.kdd.synonym.synonymRev.algorithm.JoinMHNaiveDP;
import snu.kdd.synonym.synonymRev.algorithm.JoinMHNaiveThres;
import snu.kdd.synonym.synonymRev.algorithm.JoinMHNaiveThresDP;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH_Split;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinFast;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinNaive;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinNaiveDP;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinNaiveThres;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinNaiveThresDP;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinPosition;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinRange;
import snu.kdd.synonym.synonymRev.algorithm.JoinNaive;
import snu.kdd.synonym.synonymRev.algorithm.JoinNaive_Split;
import snu.kdd.synonym.synonymRev.algorithm.JoinSetNaive;
import snu.kdd.synonym.synonymRev.algorithm.SIJoin;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinHybridAllDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMHDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMHDeltaDP;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMHNaiveDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMHNaiveThresDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMHStrongDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMinDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMinDeltaDP;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMinNaiveDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMinNaiveThresDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMinStrongDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinNaiveDelta;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinNaiveDelta2;
import snu.kdd.synonym.synonymRev.algorithm.delta.PassJoinExact;
import snu.kdd.synonym.synonymRev.algorithm.misc.EquivTest;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
import snu.kdd.synonym.synonymRev.algorithm.misc.PrintManyEstimated;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.JoinMinDP;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.JoinPQFilterDP;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.JoinBKPSet;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.JoinFKPSet;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.JoinPQFilterDPSet;
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

		boolean split = cmd.hasOption( "split" );

		switch( algorithmName ) {
		case JoinNaive:
			if( split ) {
				alg = new JoinNaive_Split( query, stat );
			}
			else {
				alg = new JoinNaive( query, stat );
			}
			break;

		case JoinMH:
			if( split ) {
				alg = new JoinMH_Split( query, stat );
			}
			else {
				alg = new JoinMH( query, stat );
			}
			break;

		case JoinBK:
			if( split ) {
				alg = new JoinBK_Split( query, stat );
			}
			else {
				alg = new JoinBK( query, stat );
			}
			break;

		case JoinMin:
			alg = new JoinMin( query, stat );
			break;

		case JoinMinFast:
			alg = new JoinMinFast( query, stat );
			break;

		case JoinMinPosition:
			alg = new JoinMinPosition( query, stat );
			break;

		case JoinMinRange:
			alg = new JoinMinRange( query, stat );
			break;

		case JoinMinNaive:
			alg = new JoinMinNaive( query, stat );
			break;

		case JoinMinNaiveThres:
			alg = new JoinMinNaiveThres( query, stat );
			break;

		case JoinMHNaive:
			alg = new JoinMHNaive( query, stat );
			break;

		case JoinMHNaiveThres:
			alg = new JoinMHNaiveThres( query, stat );
			break;

		case EstimationTest:
			alg = new EstimationTest( query, stat );
			break;

		case JoinCatesian:
			alg = new JoinCatesian( query, stat );
			break;

		case JoinHybridAll:
			alg = new JoinHybridAll( query, stat );
			break;

		case JoinHybridAll2:
			alg = new JoinHybridAll2( query, stat );
			break;

		case JoinHybridAll3:
			alg = new JoinHybridAll3( query, stat );
			break;

		case JoinHybridAll_NEW:
			alg = new JoinHybridAll_NEW( query, stat ); 
			break;

		case SIJoin:
			alg = new SIJoin( query, stat );
			break;

		case EquivTest:
			alg = new EquivTest( query, stat );
			break;

		case EstimatedOut:
			alg = new PrintManyEstimated( query, stat );
			break;

		case JoinHybridOpt:
			alg = new JoinHybridAll_NEW( query, stat );
			break;
			
		case JoinPkduck:
			alg = new JoinPkduck( query, stat );
			break;
			
		case JoinPQFilterDP:
			alg = new JoinPQFilterDP( query, stat );
			break;
		
		case JoinMHDP:
			alg = new JoinMHDP( query, stat );
			break;
		
		case JoinMHNaiveDP:
			alg = new JoinMHNaiveDP( query, stat );
			break;
		
		case JoinMHNaiveThresDP:
			alg = new JoinMHNaiveThresDP( query, stat );
			break;
		
		case JoinMinDP:
			alg = new JoinMinDP( query, stat );
			break;
		
		case JoinMinNaiveDP:
			alg = new JoinMinNaiveDP( query, stat );
			break;
		
		case JoinMinNaiveThresDP:
			alg = new JoinMinNaiveThresDP( query, stat );
			break;

		case JoinNaiveDelta:
			alg = new JoinNaiveDelta( query, stat );
			break;
		
		case JoinNaiveDelta2:
			alg = new JoinNaiveDelta2( query, stat );
			break;
		
		case JoinMHDelta:
			alg = new JoinMHDelta( query, stat );
			break;
		
		case JoinMHStrongDelta:
			alg = new JoinMHStrongDelta( query, stat );
			break;
		
		case JoinMHNaiveDelta:
			alg = new JoinMHNaiveDelta( query, stat );
			break;
		
		case JoinMHNaiveThresDelta:
			alg = new JoinMHNaiveThresDelta( query, stat );
			break;
		
		case JoinMHDeltaDP:
			alg = new JoinMHDeltaDP( query, stat );
			break;
		
		case JoinMinDelta:
			alg = new JoinMinDelta( query, stat );
			break;
		
		case JoinMinStrongDelta:
			alg = new JoinMinStrongDelta( query, stat );
			break;
		
		case JoinMinNaiveDelta:
			alg = new JoinMinNaiveDelta( query, stat );
			break;
		
		case JoinMinNaiveThresDelta:
			alg = new JoinMinNaiveThresDelta( query, stat );
			break;
		
		case JoinMinDeltaDP:
			alg = new JoinMinDeltaDP( query, stat );
			break;
		
		case JoinHybridAllDelta:
			alg = new JoinHybridAllDelta( query, stat );
			break;
		
		case JoinPkduckSet:
			alg = new JoinPkduckSet( query, stat );
			break;
		
		case JoinPQFilterDPSet:
			alg = new JoinPQFilterDPSet( query, stat );
			break;

		case JoinFKPSet:
			alg = new JoinFKPSet( query, stat );
			break;

		case JoinBKPSet:
			alg = new JoinBKPSet ( query, stat );
			break;

		case JoinSetNaive:
			alg = new JoinSetNaive( query, stat );
			break;
		case PassJoinExact:
			alg = new PassJoinExact( query, stat );
			break;
		
		
		default:
			Util.printLog( "Invalid algorithm " + algorithmName );
			System.exit( 0 );
			break;
		}
		
		// if query is not a self join, conduct semi-unidirectional join.
		if ( !query.selfJoin && !query.oneSideJoin ) alg = new AlgorithmSemiUniWrapper( (AlgorithmTemplate)alg );

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
