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
import snu.kdd.synonym.synonymRev.validator.Validator;

public class PrintManyEstimated extends AlgorithmTemplate {
	Validator val;

	public PrintManyEstimated( Query query, StatContainer stat ) throws IOException {
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

		preprocess();

		for( Record x : query.searchedSet.get() ) {
			if( x.getEstNumTransformed() > 10000000 ) {
				System.out.println( x );

				for( int i = 0; i < x.applicableRules.length; i++ ) {
					System.out.println( i );
					for( int j = 0; j < x.applicableRules[ i ].length; j++ ) {
						if( !x.applicableRules[ i ][ j ].isSelfRule() ) {
							System.out.println( x.applicableRules[ i ][ j ].toOriginalString( query.tokenIndex ) );
						}
					}
				}

				System.out.println( "\n" );
			}
		}
	}
}
