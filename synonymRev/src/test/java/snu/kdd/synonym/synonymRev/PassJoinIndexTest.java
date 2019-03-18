package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import passjoin.PassJoinIndexForSynonyms;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class PassJoinIndexTest {
	
	static Query query;
	
	@BeforeClass
	public static void getQuery() throws IOException {
		query = TestUtils.getTestQuery( 1000 );
	}

	@Test
	public void test() {
		int deltaMax = 1;
		PassJoinIndexForSynonyms index = new PassJoinIndexForSynonyms( query, deltaMax, new StatContainer() );
		ResultSet rslt = index.join(query, null, null, true );
		System.out.println( rslt.size() );
	}

}
