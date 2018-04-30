package snu.kdd.synonym.synonymRev.algorithm.misc;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class SampleDataTest {
	
	private static final Boolean debug = false;
	public final Query query;
	public final ObjectArrayList<Record> sample_records;
	public final ObjectOpenHashSet<Rule> sample_rules;
	public final ObjectArrayList<ObjectOpenHashSet<QGram>> sample_pos_qgrams;

	public SampleDataTest(int q) throws IOException {
		/* preprocessing */
		//"D:\\ghsong\\data\\yjpark_data\\data1_1000000_5_10000_1.0_0.0_1.txt";
		//"D:\\ghsong\\data\\yjpark_data\\data2_1000000_5_15848_1.0_0.0_2.txt";
		//"D:\\ghsong\\data\\yjpark_data\\rule1_30000_2_2_10000_0.0_0.txt";
		//"D:\\ghsong\\data\\yjpark_data\\rule2_30000_2_2_30000_0.0_0.txt";


		final String dataOnePath = "D:\\ghsong\\data\\yjpark_data\\data1_1000000_5_10000_1.0_0.0_1.txt";
		final String dataTwoPath = "D:\\ghsong\\data\\yjpark_data\\data2_1000000_5_15848_1.0_0.0_2.txt";
		final String rulePath = "D:\\ghsong\\data\\yjpark_data\\rule2_30000_2_2_30000_0.0_0.txt";
		final String outputPath = "output";
		final Boolean oneSideJoin = true;
		query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
//		final int q = 2;
		
		//[24397 3252 10978 5663]
  	
		/* DEBUG: additional data */
//		Rule rule0 = new Rule( new int[] {24397, 3252, 10978}, new int[] {777} );
		//System.out.println( rule0.toOriginalString( query.tokenIndex ) );
//		query.ruleSet.add(rule0);

		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		//System.exit(1);
		
		for (Record record : query.searchedSet.recordList) {
			record.preprocessRules( automata );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
		}
		
		
		/* sample some records and test the DP algorithm. */
//		final int[] idxArray = {2, 5, 8077, 11165, 12444};
		final int[] idxArray = {};
		final IntOpenHashSet idxSet = new IntOpenHashSet(idxArray);
		for (int i=0; i<2000; i++) idxSet.add( i );
		final int pos_max = 30;
		sample_records = new ObjectArrayList<Record>();
		sample_rules = new ObjectOpenHashSet<Rule>();
		sample_pos_qgrams = new ObjectArrayList<ObjectOpenHashSet<QGram>>();
		for (int i=0; i<pos_max; i++) sample_pos_qgrams.add( new ObjectOpenHashSet<QGram>() );

		int idx = 0;
		for ( final Record record : query.indexedSet.get()) {
			if (idxSet.contains( idx )) {
				record.preprocessRules(automata);
				record.preprocessSuffixApplicableRules();
				record.preprocessTransformLength();
				if (debug) System.out.println("idx: "+idx);
				if (debug) System.out.println("record: "+Arrays.toString( record.getTokensArray() ));
				sample_records.add( record );

				if (debug) System.out.println( "applicable rules: " );
				for (int pos=0; pos<record.size(); pos++ ) {
					for (final Rule rule : record.getSuffixApplicableRules( pos )) {
						if (debug) System.out.println("\t("+rule.toString()+", "+pos+")");
						sample_rules.add( rule );
					}
				}

				if (debug) System.out.println( "transformed strings: " );
				final List<Record> expanded = record.expandAll();
				for( final Record exp : expanded ) {
					if (debug) System.out.println( "\t"+Arrays.toString( exp.getTokensArray() ) );
				}
				
				if (debug) System.out.println( "positional q-grams: " );
				List<List<QGram>> qgrams_self = record.getSelfQGrams( q, record.getTokenCount() );
				for (int i=0; i<qgrams_self.size(); i++) {
					for (final QGram qgram : qgrams_self.get(i)) {
						if (debug) System.out.println( "\t["+qgram.toString()+", "+i+"]" );
					}
				}
				
				if (debug) System.out.println( "positional q-grams in a transformed string: " );
				List<List<QGram>> qgrams = record.getQGrams(q);
				for (int i=0; i<qgrams.size(); i++) {
					for (final QGram qgram : qgrams.get(i)) {
						if (debug) System.out.println( "\t["+qgram.toString()+", "+i+"]" );
						sample_pos_qgrams.get( i ).add( qgram );
					}
				}
				
			} // end if idxSet.contains

			idx++;
			if (idx > 20000) break;
		}

		/* check the retrieved data */
//		check_samples( query, sample_records, sample_rules, sample_pos_qgrams );
		
//		System.out.println( "Number of records: "+sample_records.size() );
//		System.out.println( "Number of rules: "+sample_rules.size() );
//		int n_pos_qgrams = 0;
//		for (int i=0; i<sample_pos_qgrams.size(); i++) n_pos_qgrams += sample_pos_qgrams.get( i ).size();
//		System.out.println( "Number of pos q-grams: "+n_pos_qgrams);
		
		System.out.println( "Number of records in S: "+query.searchedSet.size() );
		System.out.println( "Number of records in T: "+query.indexedSet.size() );
		System.out.println( "Number of rules: "+query.ruleSet.size() );

	}

	public static void check_samples(Query query, ObjectList<Record> sample_records, ObjectSet<Rule> sample_rules, 
			ObjectList<ObjectOpenHashSet<QGram>> sample_pos_qgrams) {
		System.out.println( "records: "+sample_records.size() );
		for (final Record record : sample_records) {
			System.out.println( Arrays.toString( record.getTokensArray() ) );
		}
		System.out.println(  );
		
		System.out.println( "rules: "+sample_rules.size() );
		for (final Rule rule : sample_rules) {
			System.out.println( rule.toString());
		}
		System.out.println(  );
		
		System.out.println( "pos qgrams: ");
		for (int i=0; i<sample_pos_qgrams.size(); i++) {
			for (final QGram qgram : sample_pos_qgrams.get(i)) {
				System.out.println( "["+qgram.toString()+", "+i+"]" );
			}
		}
	}

	public static void inspect_record(final Record record, final Query query, final int q) {
		//System.out.println("record: "+record.toString(query.tokenIndex));
		System.out.println("record: "+Arrays.toString(record.getTokensArray()) );

		System.out.println( "applicable rules: " );
		for (int pos=0; pos<record.size(); pos++ ) {
			for (final Rule rule : record.getSuffixApplicableRules( pos )) {
				//System.out.println("\t("+rule.toOriginalString(query.tokenIndex)+", "+pos+")");
				System.out.println("\t("+rule.toString()+", "+pos+")");
			}
		}

		System.out.println( "transformed strings: " );
		final List<Record> expanded = record.expandAll();
		for( final Record exp : expanded ) {
			System.out.println( "\t"+Arrays.toString( exp.getTokensArray() ) );
		}
		
		System.out.println( "positional q-grams: " );
		List<List<QGram>> qgrams_self = record.getSelfQGrams( q, record.getTokenCount() );
		for (int i=0; i<qgrams_self.size(); i++) {
			for (final QGram qgram : qgrams_self.get(i)) {
				//System.out.println( "\t["+qgram.toString( query.tokenIndex )+", "+i+"]" );
				System.out.println( "\t["+qgram.toString()+", "+i+"]" );
			}
		}
		
		System.out.println( "positional q-grams in a transformed string: " );
		List<List<QGram>> qgrams = record.getQGrams(q);
		for (int i=0; i<qgrams.size(); i++) {
			for (final QGram qgram : qgrams.get(i)) {
				//System.out.println( "\t["+qgram.toString( query.tokenIndex )+", "+i+"]" );
				System.out.println( "\t["+qgram.toString()+", "+i+"]" );
			}
		}
	}
}