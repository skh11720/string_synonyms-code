package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;

public class TestUtils {

	public static Query getTestQuery() throws IOException {
		String osName = System.getProperty( "os.name" );
		Query query = null;
		if ( osName.startsWith( "Windows" ) ) {
			final String dataOnePath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
			final String dataTwoPath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
			final String rulePath = "D:\\ghsong\\data\\wordnet\\rules.noun";
			final String outputPath = "output";
			final Boolean oneSideJoin = true;
			query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		}
		else if ( osName.startsWith( "Linux" ) ) {
			final String dataOnePath = "run/data_store/aol/splitted/aol_1000_data.txt";
			final String dataTwoPath = "run/data_store/aol/splitted/aol_1000_data.txt";
			final String rulePath = "run/data_store/wordnet/rules.noun";
			final String outputPath = "output";
			final Boolean oneSideJoin = true;
			query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		}

//		record = query.searchedSet.getRecord( 0 );
		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		for ( Record record : query.searchedSet.recordList ) {
			record.preprocessRules( automata );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
			record.preprocessTransformLength();
			record.preprocessEstimatedRecords();
		}
		return query;
	}
}
