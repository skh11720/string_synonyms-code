package vldb17.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;
import snu.kdd.synonym.synonymRev.order.FrequencyFirstOrder;
import snu.kdd.synonym.synonymRev.order.PositionFirstOrder;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.NaiveOneSide;
import vldb17.GreedyValidator;
import vldb17.seq.JoinPkduck;
import vldb17.seq.PkduckDP;
import vldb17.seq.PkduckDPWithRC;
import vldb17.seq.PkduckIndex;
import vldb17.seq.RCTableSeq;

public class RCTableTest {
	
	public static Query query;
	public static AbstractGlobalOrder globalOrder;
	
	public static void loadData() throws IOException {
		
		// synthetic 
//		final String dataOnePath = "D:\\ghsong\\data\\yjpark_data\\data\\1000000_5_15848_1.0_0.0_1.txt";
//		final String dataTwoPath = "D:\\ghsong\\data\\yjpark_data\\data\\1000000_5_15848_1.0_0.0_2.txt";
//		final String rulePath = "D:\\ghsong\\data\\yjpark_data\\rule\\30000_2_2_10000_0.0_0.txt";

		
		// USPS
//		final String dataOnePath = "D:\\ghsong\\data\\JiahengLu\\splitted\\USPS_10000.txt";
//		final String dataTwoPath = "D:\\ghsong\\data\\JiahengLu\\splitted\\USPS_10000.txt";
//		final String rulePath = "D:\\ghsong\\data\\JiahengLu\\USPS_rule.txt";

		// AOL
		final String dataOnePath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
		final String dataTwoPath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
		final String rulePath = "D:\\ghsong\\data\\wordnet\\rules.noun";
		
		// SPROT
//		final String dataOnePath = "D:\\ghsong\\data\\sprot\\splitted\\SPROT_two_15848.txt";
//		final String dataTwoPath = "D:\\ghsong\\data\\sprot\\splitted\\SPROT_two_15848.txt";
//		final String rulePath = "D:\\ghsong\\data\\sprot\\rule.txt";

		

//		final String rulePath = "D:\\ghsong\\data\\yjpark_data\\rule2_30000_2_2_30000_0.0_0.txt";
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
		
		System.out.println( "Number of records in S: "+query.searchedSet.size() );
		System.out.println( "Number of records in T: "+query.indexedSet.size() );
		System.out.println( "Number of rules: "+query.ruleSet.size() );
	}
	
	public static void tableTest() {
		Record rec = query.searchedSet.getRecord( 1 );
		System.out.println( rec );
		System.out.println( Arrays.toString( rec.getTokensArray() ) );
		RCTableSeq table = new RCTableSeq( rec, globalOrder );
	}
	
	public static void main( String[] args ) throws IOException, ParseException {
		loadData();
//		GlobalOrder[] globalOrderList = {GlobalOrder.PF, GlobalOrder.TF};
//		GlobalOrder[] globalOrderList = {GlobalOrder.PF};
		int qSize = 2;
		Ordering[] orderList = {Ordering.TF};
		for (Ordering order: orderList) {
			AbstractGlobalOrder globalOrder;
			switch (order) {
			case PF: globalOrder = new PositionFirstOrder(qSize); break;
			case FF: globalOrder = new FrequencyFirstOrder(qSize); break;
			default: throw new RuntimeException("Unexpected error");
			}
			RCTableTest.globalOrder = globalOrder;
			System.out.println( "Global order: "+globalOrder.getMode() );
			tableTest();
		}
	}
}
