package snu.kdd.synonym.synonymRev.algorithm;

import org.apache.commons.cli.CommandLine;

import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaNaive;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaSimple;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVar;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVarBK;
import snu.kdd.synonym.synonymRev.algorithm.set.JoinBKPSet;
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

	public static AlgorithmInterface getAlgorithmInstance( CommandLine cmd, boolean isSelfJoin ) {
		AlgorithmInterface alg = null;
		AlgorithmName algorithmName = AlgorithmName.valueOf( cmd.getOptionValue( "algorithm" ) );

		String additionalOptions = cmd.getOptionValue( "additional", "" );
		String[] additionalArgs = null;
		if( additionalOptions != null ) additionalArgs = additionalOptions.split( " " );

		switch( algorithmName ) {
		case JoinNaive:
			alg = new JoinNaive( additionalArgs );
			break;

		case JoinMH:
			alg = new JoinMH( additionalArgs );
			break;

		case JoinMin:
			alg = new JoinMin( additionalArgs );
			break;

		case JoinMinFast:
			alg = new JoinMinFast( additionalArgs );
			break;

		case JoinHybridAll:
			alg = new JoinHybridAll( additionalArgs );
			break;

		case SIJoin:
			alg = new SIJoin( additionalArgs );
			break;

		case SIJoinOriginal:
			alg = new SIJoinOriginal( additionalArgs );
			break;

		case JoinPkduck:
			alg = new JoinPkduck( additionalArgs );
			break;

		case JoinPkduckSet:
			alg = new JoinPkduckSet( additionalArgs );
			break;

		case JoinPkduckOriginal:
			alg = new JoinPkduckOriginal( additionalArgs );
			break;

		case JoinBKPSet:
			alg = new JoinBKPSet ( additionalArgs );
			break;

		case JoinSetNaive:
			alg = new JoinSetNaive( additionalArgs );
			break;

		case PassJoin:
			alg = new PassJoin( additionalArgs );
			break;
		
		case JoinDeltaNaive:
			alg = new JoinDeltaNaive( additionalArgs );
			break;

		case JoinDeltaSimple:
			alg = new JoinDeltaSimple( additionalArgs );
			break;

		case JoinDeltaVar:
			alg = new JoinDeltaVar( additionalArgs );
			break;

		case JoinDeltaVarBK:
			alg = new JoinDeltaVarBK( additionalArgs );
			break;
		
		default:
			Util.printLog( "Invalid algorithm " + algorithmName );
			System.exit( 0 );
			break;
		}
		
		if (!isSelfJoin) alg = new AlgorithmBidirectionWrapper(alg);

		return alg;
	}
}
