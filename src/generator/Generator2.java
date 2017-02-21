package generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import mine.Record;
import tools.Rule;

/**
 * Generate strings and ruls based on zipf distribution.
 * The orders of tokens for both datasets are different.
 * Do not generate equivalent strings manually.
 */

public class Generator2 {
	private List<Integer> map2str;
	private double[] ratio;
	private Random random;

	public Generator2( int nDistinctTokens, double zipf ) {
		this( nDistinctTokens, zipf, System.currentTimeMillis() );
	}

	public Generator2( int nDistinctTokens, double zipf, long seed ) {
		map2str = new ArrayList<Integer>();
		ratio = new double[ nDistinctTokens ];
		for( int i = 0; i < nDistinctTokens; ++i ) {
			map2str.add( i );
			ratio[ i ] = 1.0 / Math.pow( i + 1, zipf );
			if( i != 0 )
				ratio[ i ] += ratio[ i - 1 ];
		}
		for( int i = 0; i < ratio.length; ++i )
			ratio[ i ] /= ratio[ ratio.length - 1 ];
		random = new Random( seed );
		Collections.shuffle( map2str, random );
	}

	private static void printUsage() {
		System.out.println( "-d <#tokens> <#avg length> <#records> <skewness> : generate data" );
		System.out.println( "-r <#tokens> <max lhs len> <max rhs len> <#rules> <skewness> : generate rule" );
		System.exit( 1 );
	}

	public static void main( String[] args ) throws IOException {
		if( args[ 0 ].compareTo( "-d" ) == 0 ) {
			int nTokens = Integer.parseInt( args[ 1 ] );
			int avgRecLen = Integer.parseInt( args[ 2 ] );
			int nRecords = Integer.parseInt( args[ 3 ] );
			double skewZ = Double.parseDouble( args[ 4 ] );
			Generator2 gen = new Generator2( nTokens, skewZ );
			gen.genString( avgRecLen, nRecords, new File( "data" ) );
		}
		else if( args[ 0 ].compareTo( "-r" ) == 0 ) {
			int nTokens = Integer.parseInt( args[ 1 ] );
			int avgLhsLen = Integer.parseInt( args[ 2 ] );
			int avgRhsLen = Integer.parseInt( args[ 3 ] );
			int nRules = Integer.parseInt( args[ 4 ] );
			double skewZ = Double.parseDouble( args[ 5 ] );
			Generator2 gen = new Generator2( nTokens, skewZ );
			gen.genSkewRule( avgLhsLen, avgRhsLen, nRules, new File( "rule" ) );
		}
		else {
			printUsage();
		}
	}

	public void genString( int avgLength, int nRecords, File file ) throws IOException {
		HashSet<Record> records = new HashSet<Record>();
		int count = 0;
		while( records.size() < nRecords ) {
			Record rec = randomString( avgLength );
			records.add( rec );
		}
		BufferedWriter bw = new BufferedWriter( new FileWriter( file ) );
		for( Record rec : records ) {
			int[] arr = rec.getTokenArray();
			String str = "" + arr[ 0 ];
			for( int i = 1; i < arr.length; ++i )
				str += " " + arr[ i ];
			bw.write( str + "\n" );
		}
		bw.close();
		System.out.println( count );
	}

	private Record randomString( int avgLength ) {
		// 1. sample length of string
		int len = (int) Math.max( 1, avgLength + random.nextGaussian() );
		// 2. generate random string
		int[] tokens = random( len );
		Record rec = new Record( tokens );
		return rec;
	}

	// generate random string
	private int[] random( int len ) {
		Set<Integer> samples = new HashSet<Integer>();
		int[] tokens = new int[ len ];
		// 2. generate random string
		while( samples.size() < len ) {
			double rd = random.nextDouble();
			int token = Arrays.binarySearch( ratio, rd );
			if( token < 0 )
				token = -token - 1;
			if( samples.contains( token ) )
				continue;
			tokens[ samples.size() ] = token;
			samples.add( token );
		}
		for( int i = 0; i < len; ++i )
			tokens[ i ] = map2str.get( tokens[ i ] );
		return tokens;
	}

	// Rules: do not follow zipf distribution. Use uniform distribution
	public void genSkewRule( int lhsmax, int rhsmax, int nRules, File file ) throws IOException {
		HashSet<Rule> rules = new HashSet<Rule>();
		while( rules.size() < nRules ) {
			// 1. sample length of lhs and rhs
			int lhslen = random.nextInt( lhsmax ) + 1;
			int rhslen = random.nextInt( rhsmax ) + 1;
			// 2. generate random lhs
			int[] from = random( lhslen );
			int[] to = random( rhslen );
			if( lhslen == rhslen ) {
				boolean equals = true;
				for( int t = 0; t < lhslen; ++t )
					if( from[ t ] != to[ t ] )
						equals = false;
				if( equals )
					continue;
			}
			Rule rule = new Rule( from, to );
			rules.add( rule );
		}
		BufferedWriter bw = new BufferedWriter( new FileWriter( file ) );
		for( Rule rule : rules ) {
			for( int from : rule.getFrom() )
				bw.write( from + " " );
			bw.write( ", " );
			for( int to : rule.getTo() )
				bw.write( to + " " );
			bw.newLine();
		}
		bw.close();
	}
}
