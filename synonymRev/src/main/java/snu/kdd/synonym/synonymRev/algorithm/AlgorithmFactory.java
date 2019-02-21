package snu.kdd.synonym.synonymRev.algorithm;

import org.apache.commons.cli.CommandLine;

import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaNaive;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaSimple;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVar;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVarBK;
import snu.kdd.synonym.synonymRev.algorithm.set.JoinBKPSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.Util;
import vldb17.seq.JoinPkduck;
import vldb17.set.JoinPkduckOriginal;
import vldb17.set.JoinPkduckSet;

public class AlgorithmFactory {

	private enum AlgorithmName {
		JoinNaive,
		JoinMH,
		JoinMin,
		JoinMinFast,
		JoinHybridAll,
		SIJoin,
		JoinPkduck,
		
		SIJoinOriginal,
		JoinPkduckOriginal,

		JoinSetNaive,
		JoinPkduckSet,
		JoinBKPSet,
		PassJoin,
		
		JoinDeltaNaive,
		JoinDeltaSimple,
		JoinDeltaVar,
		JoinDeltaVarBK,
	}

	public static AlgorithmInterface getAlgorithmInstance(Query query, CommandLine cmd ) {
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
