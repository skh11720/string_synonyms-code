package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;
import vldb17.GreedyValidatorOriginal;

public class PkduckGreedyValidatorTest {

	@Test
	public void test() throws IOException {
		
		String dataset = "POLY_gene_ontology";
		Query query = Util.getQueryWithPreprocessing(dataset, 0);
		Record.tokenIndex = query.tokenIndex;
		double theta = 1.0;
		
		int sid = 10297;
		int tid = 35596;
		
		Record s = query.searchedSet.getRecord(sid);
		Record t = query.indexedSet.getRecord(tid);
		
		System.out.println("s: "+s.toString(query.tokenIndex));
		System.out.println("t: "+t.toString(query.tokenIndex));
		
		
		GreedyValidatorOriginal checker = new GreedyValidatorOriginal(theta);
		checker.isEqual(s, t);
	}

}
