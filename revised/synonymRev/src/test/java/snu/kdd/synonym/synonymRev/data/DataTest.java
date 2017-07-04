package snu.kdd.synonym.synonymRev.data;

import java.util.ArrayList;

import org.junit.Test;

public class DataTest {

	@Test
	public void test() {
		ArrayList<Rule> ruleList = new ArrayList<Rule>();

		int[] lhs = new int[ 3 ];
		int[] rhs = new int[ 2 ];

		lhs[ 0 ] = 1;
		lhs[ 1 ] = 2;
		lhs[ 2 ] = 3;

		rhs[ 0 ] = 10;
		rhs[ 1 ] = 11;

		ruleList.add( new Rule( lhs, rhs ) );

		ACAutomataR automata = new ACAutomataR( ruleList );
	}

}
