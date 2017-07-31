package snu.kdd.synonym.synonymRev.data;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import snu.kdd.synonym.synonymRev.tools.QGram;

public class DataTest {

	@Test
	public void test() {
		ArrayList<Rule> ruleList = new ArrayList<Rule>();

		int[] lhs1 = new int[ 3 ];
		int[] rhs1 = new int[ 2 ];

		lhs1[ 0 ] = 1;
		lhs1[ 1 ] = 2;
		lhs1[ 2 ] = 3;

		rhs1[ 0 ] = 10;
		rhs1[ 1 ] = 11;

		ruleList.add( new Rule( lhs1, rhs1 ) );

		int[] lhs2 = new int[ 2 ];
		lhs2[ 0 ] = 2;
		lhs2[ 1 ] = 3;

		int[] rhs2 = new int[ 3 ];
		rhs2[ 0 ] = 12;
		rhs2[ 1 ] = 13;
		rhs2[ 2 ] = 14;

		ruleList.add( new Rule( lhs2, rhs2 ) );

		int[] lhs3 = new int[ 1 ];
		lhs3[ 0 ] = 4;

		int[] rhs3 = new int[ 3 ];
		rhs3[ 0 ] = 15;
		rhs3[ 1 ] = 16;
		rhs3[ 2 ] = 17;

		ruleList.add( new Rule( lhs3, rhs3 ) );

		ACAutomataR automata = new ACAutomataR( ruleList );

		int[] tokens = { 1, 2, 3, 4 };
		Record r = new Record( tokens );

		r.preprocessRules( automata );
		r.preprocessTransformLength();

		int maxTransformed = r.getMaxTransLength();
		int minTransformed = r.getMinTransLength();

		assertTrue( maxTransformed == 7 );
		assertTrue( minTransformed == 3 );

		List<Record> transformed = r.expandAll();
		
		assertTrue( transformed.size() == 6 );

		List<List<QGram>> qgramList = r.getQGrams( 2 );
		int[] sizeArray = { 3, 4, 5, 5, 4, 2, 1 };
		for( int i = 0; i < qgramList.size(); i++ ) {
			List<QGram> list = qgramList.get( i );
			assertTrue( sizeArray[ i ] == list.size() );
		}
	}
}
