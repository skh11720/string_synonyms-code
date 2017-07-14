package snu.kdd.synonym.synonymRev.data;

import java.util.ArrayList;

import org.junit.Test;

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

		ACAutomataR automata = new ACAutomataR( ruleList );

		int[] tokens = { 1, 2, 3, 4 };

		Rule[][] applicable = automata.applicableRules( tokens );
		for( int i = 0; i < applicable.length; i++ ) {
			for( int j = 0; j < applicable[ i ].length; j++ ) {
				System.out.println( applicable[ i ][ j ] );
			}
		}

	}

}
