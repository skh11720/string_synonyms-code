package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.junit.Test;

public class RuleQualityTest {
	
	@Test
	public void test() throws IOException {
		String dataOnePath = "D:/ghsong/data/synonyms/PolySearch2/polysearch2_data.txt";
		String dataTwoPath = dataOnePath;
		String rulePath = "D:/ghsong/data/synonyms/sprot/rule_uniq.txt";

		boolean oneSideJoin = true;
		String outputPath = "output";
		Query query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		
		int[] arrCountAppliedRules = new int[query.ruleSet.size()];
		Arrays.fill(arrCountAppliedRules, 0);
		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		
		for ( Record record : query.searchedSet.recordList ) {
			record.preprocessRules( automata );
//			System.out.println(record);
			for ( int k=0; k<record.size(); ++k ) {
				for ( Rule rule : record.getApplicableRules(k) ) {
//					System.out.println("RULE: "+ridx+"\t"+Arrays.toString(rule.getLeft()) );
//					System.out.println("RECORD:"+record.getID()+"\t"+Arrays.toString(record.getTokensArray()) );
					if ( rule.isSelfRule ) continue;
					if ( Arrays.equals( rule.getLeft(), record.getTokensArray() ) ) continue;
					int ridx = rule.id;
					arrCountAppliedRules[ridx] += 1;
				}
			}
		}
		
		/*
		 * Output rules which have the "count" larger than 0.
		 */
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/rule_refined.txt")));
		for ( int ridx=0; ridx<arrCountAppliedRules.length; ++ridx ) {
			if ( arrCountAppliedRules[ridx] == 0 ) continue;
			Rule rule = query.ruleSet.ruleList.get(ridx);
//			rule.getLeft()
			String lhs = query.tokenIndex.toString( rule.getLeft() );
			String rhs = query.tokenIndex.toString( rule.getRight() );
			pw.println(lhs +", "+rhs);
		}
		pw.close();
	}
	
	
}
