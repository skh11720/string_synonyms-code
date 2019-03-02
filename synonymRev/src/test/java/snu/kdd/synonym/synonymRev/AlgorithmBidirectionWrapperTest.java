package snu.kdd.synonym.synonymRev;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaNaive;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class AlgorithmBidirectionWrapperTest {

	@Test
	public void test() throws IOException, ParseException {
		Query query = TestUtils.getTestQuery("UNIV_1_2", 0);
		runAlg(query);
		query = TestUtils.getTestQuery("UNIV_2_1", 0);
		runAlg(query);
	}
	
	private void runAlg( Query query ) throws ParseException, IOException {
		String[] args = {"-algorithm", "JoinDeltaNaive", "-oneSideJoin", "True", "-additional", "-delta 1 -dist edit"};
		CommandLine cmd = App.parseInput( args );
		AlgorithmInterface alg = new JoinDeltaNaive(args[5].split(" "));

		/////////// body of AlgorithmBidirectionWrapper /////////////////
		Query queryRev = new Query( query.getRulePath(), query.getSearchedPath(), query.getIndexedPath(), query.oneSideJoin, query.outputPath );
//		Query queryRev = TestUtils.getTestQuery("UNIV_2_1", 0);

		// searchedSet -> indexedSet
		alg.run(query);
		Set<IntegerPair> rslt1 = alg.getResult();
		StatContainer stat1 = alg.getStat();
		long v_st_1 = stat1.getLong("Val_Comparisons");
		
		// indexedSet -> searchedSet
		alg.run(queryRev);
		Set<IntegerPair> rslt2 = alg.getResult();
		StatContainer stat2 = alg.getStat();
		long v_st_2 = stat2.getLong("Val_Comparisons");
		
		System.out.println("v_st_1: "+v_st_1);
		System.out.println("v_st_2: "+v_st_2);
	}

}
