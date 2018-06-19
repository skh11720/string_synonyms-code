package snu.kdd.synonym.synonymRev.validator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.synonym.synonymRev.App;
import snu.kdd.synonym.synonymRev.TestUtils;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;

public class NaiveOneSideDelta extends Validator {
	
	private final int deltaMax;
	
	public NaiveOneSideDelta( int deltaMax ) {
		this.deltaMax = deltaMax;
	}

	public int isEqual( Record x, Record y ) {
		// If there is no pre-expanded records, do expand
		++checked;
//		if( x.equals( y ) ) {
		if (Arrays.equals( x.getTokensArray(), y.getTokensArray() )) {
			return 0;
		}

		List<Record> expandedX = x.expandAll();

		for ( int d=0; d<=deltaMax; ++d ) {
			for ( int dx=0; dx<=d; ++dx ) {
				/*
				 * dx errors from xPrime, dy = d-dx errors from y
				 */
				int dy = d - dx;
				if ( dy > y.size() ) continue;
				List<IntArrayList> combList_y = Util.getCombinations( y.size(), y.size()-dy );
				for( Record xPrime : expandedX ) {
					if ( dx > xPrime.size() ) continue;
					List<IntArrayList> combList_x = Util.getCombinations( xPrime.size(), xPrime.size()-dx );
					
					for ( IntArrayList comb_x : combList_x ) {
						int[] x0 = Util.getSubsequence( xPrime.getTokensArray(), comb_x );
						for ( IntArrayList comb_y : combList_y ) {
							int[] y0 = Util.getSubsequence( y.getTokensArray(), comb_y );
							if ( x0 == null && y0 == null ) return 1;
							else if ( x0 == null || y0 == null ) continue;
							else if ( Arrays.equals( x0, y0 ) ) return 1;
						}
					}
				}
			}
		}

		return -1;
	}
	
	public String getName() {
		return "NaiveOneSideValidator";
	}

	public static void main( String[] args ) throws IOException, ParseException {
		String osName = System.getProperty( "os.name" );
		final String dataOnePath, dataTwoPath, rulePath;
		if ( osName.startsWith( "Windows" ) ) {
			dataOnePath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
			dataTwoPath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
			rulePath = "D:\\ghsong\\data\\wordnet\\rules.noun";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			dataOnePath = "run/data_store/aol/splitted/aol_1000_data.txt";
			dataTwoPath = "run/data_store/aol/splitted/aol_1000_data.txt";
			rulePath = "run/data_store/wordnet/rules.noun";
		}
		else dataOnePath = dataTwoPath = rulePath = null;
		args = ("-dataOnePath " + dataOnePath + " " + 
				"-dataTwoPath " + dataTwoPath + " " +
				"-rulePath " + rulePath + " " +
				"-outputPath output -algorithm * -oneSideJoin True -additional *").split( " ", 14 );
		
		CommandLine cmd = App.parseInput( args );
		Query query = App.getQuery( cmd );
		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		for ( Record record : query.searchedSet.recordList ) {
			record.preprocessRules( automata );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
		}

		int delta = 1;
		NaiveOneSideDelta validator = new NaiveOneSideDelta( delta );
		NaiveOneSide naiveValidator = new NaiveOneSide();
		int i = 646;
		int j = 1005;
		Record x = query.searchedSet.getRecord( i );
		Record y = query.searchedSet.getRecord( j );
		System.out.println( i+", "+j );
		System.out.println( "x: "+x );
		System.out.println( "y: "+y );
		System.out.println( "n_xExpand: "+x.expandAll().size() );
		if ( naiveValidator.isEqual( x, y ) < 0 && validator.isEqual( x, y ) >= 0 ) {
			System.out.println( "x: "+x );
			System.out.println( "y: "+y );
			System.out.println( "equivalent with "+delta+" errors" );
			for ( Record xPrime : x.expandAll() ) {
				System.out.println( xPrime );
			}
			return;
		}
	}
}
