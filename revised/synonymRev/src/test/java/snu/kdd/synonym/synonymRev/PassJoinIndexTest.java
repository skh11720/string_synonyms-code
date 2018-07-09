package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import passjoin.PassJoinIndex;
import passjoin.PassJoinIndexForSynonyms;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;

public class PassJoinIndexTest {
	
	static Query query;
	
	@BeforeClass
	public static void getQuery() throws IOException {
		query = TestUtils.getTestQuery( 1000 );
	}

	@Test
	public void test() {
		int deltaMax = 1;
		PassJoinIndexForSynonyms index = new PassJoinIndexForSynonyms( query, deltaMax );
		Set<IntegerPair> rslt = index.run();
		System.out.println( rslt.size() );
	}

}
