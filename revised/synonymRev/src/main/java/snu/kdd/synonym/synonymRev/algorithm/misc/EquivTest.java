package snu.kdd.synonym.synonymRev.algorithm.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class EquivTest extends AlgorithmTemplate {
	Validator val;

	public EquivTest( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();
	}

	@Override
	public String getName() {
		return "EquivTest";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );

		val = params.validator;
		String valName = val.getName();
		boolean topDown = valName.contains( "TopDown" );

		preprocess();

		int seed = 0;
		double threshold = 0.5;
		ArrayList<Record> sampleX = new ArrayList<>();
		Random rn = new Random( seed );
		for( Record x : query.searchedSet.get() ) {
			if( x.getEstNumTransformed() > 100 ) {
				continue;
			}

			if( rn.nextDouble() > threshold ) {
				sampleX.add( x );
			}
		}

		ArrayList<Record> sampleY = new ArrayList<>();
		for( Record y : query.searchedSet.get() ) {
			if( y.getEstNumTransformed() > 100 ) {
				continue;
			}

			if( rn.nextDouble() > threshold ) {
				sampleY.add( y );
			}
		}

		if( sampleX.size() < 100 && sampleY.size() < 100 ) {
			Util.printLog( "Too small dataset" );
			return;
		}

		StopWatch watch = StopWatch.getWatchStarted( "Total Time" );
		int count = 0;
		for( int i = 0; i < 100; i++ ) {

			Record x = sampleX.get( i );
			if( topDown ) {
				x.preprocessSuffixApplicableRules();
			}

			for( int j = 0; j < 100; j++ ) {
				if( query.selfJoin ) {
					if( i == j ) {
						continue;
					}
				}

				Record y = sampleY.get( j );

				if( query.selfJoin ) {
					if( y.getSuffixApplicableRules( 0 ) != null && topDown ) {
						y.preprocessSuffixApplicableRules();
					}
				}

				if( val.isEqual( x, y ) > 0 ) {
					count++;
				}
			}
		}
		watch.stopQuietAndAdd( stat );

		Util.printLog( val.getName() + " " + count + " " + watch.getTotalTime() );
	}

}
