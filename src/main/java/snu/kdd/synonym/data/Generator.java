package snu.kdd.synonym.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import mine.Record;
import tools.Rule;
import tools.Rule_ACAutomata;

/**
 * Generate strings and ruls based on zipf distribution.
 * The orders of tokens for both datasets are exactly the same.
 * Manually generate equivalent strings.
 */

public class Generator {
	private double[] ratio;
	private Random random;
	private Map<String, Integer> str2int;
	private List<String> int2str;
	private long seed;
	private double zipf;
	private int nTokens;

	public Generator( int nDistinctTokens, double zipf ) {
		this( nDistinctTokens, zipf, System.currentTimeMillis() );
	}

	public Generator( int nDistinctTokens, double zipf, long seed ) {
		this.seed = seed;

		this.nTokens = nDistinctTokens;
		this.zipf = zipf;

		ratio = new double[ nDistinctTokens ];
		for( int i = 0; i < ratio.length; ++i ) {
			ratio[ i ] = 1.0 / Math.pow( i + 1, zipf );
			if( i != 0 )
				ratio[ i ] += ratio[ i - 1 ];
		}
		for( int i = 0; i < ratio.length; ++i )
			ratio[ i ] /= ratio[ ratio.length - 1 ];
		random = new Random( seed );
		str2int = new HashMap<String, Integer>();
		int2str = new ArrayList<String>();
		for( int i = 0; i < nDistinctTokens; ++i ) {
			str2int.put( i + "", i );
			int2str.add( i + "" );
		}
	}

	private static void printUsage() {
		System.out.println(
				"-d <rulefile> <#tokens> <#avg length> <#records> <skewness> <equiv ratio>: generate data with given rulefile" );
		System.out.println( "-r <#tokens> <max lhs len> <max rhs len> <#rules>: generate rule" );
		System.exit( 1 );
	}

	public static void main( String[] args ) throws IOException {
		// build rule first and then generate data
		if( args[ 0 ].equals( "-r" ) ) {
			int nTokens = Integer.parseInt( args[ 1 ] );
			int avgLhsLen = Integer.parseInt( args[ 2 ] );
			int avgRhsLen = Integer.parseInt( args[ 3 ] );
			int nRules = Integer.parseInt( args[ 4 ] );
			double skewZ = Double.parseDouble( args[ 5 ] );

			Generator gen = new Generator( nTokens, skewZ );
			gen.genSkewRule( avgLhsLen, avgRhsLen, nRules, new File( "rule" ) );

		}
		else if( args[ 0 ].equals( "-d" ) ) {
			String rulefile = args[ 1 ];
			int nTokens = Integer.parseInt( args[ 2 ] );
			int avgRecLen = Integer.parseInt( args[ 3 ] );
			int nRecords = Integer.parseInt( args[ 4 ] );
			double skewZ = Double.parseDouble( args[ 5 ] );
			double equivratio = Double.parseDouble( args[ 6 ] );

			Generator gen = new Generator( nTokens, skewZ );
			Rule_ACAutomata atm = gen.readRules( rulefile );
			gen.genString( avgRecLen, nRecords, new File( "data" ), equivratio, atm );
		}
		else {
			printUsage();
		}
	}

	public Rule_ACAutomata readRules( String rulefile ) throws IOException {
		List<Rule> rulelist = new ArrayList<Rule>();
		BufferedReader br = new BufferedReader( new FileReader( rulefile ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			Rule rule = new Rule( line, str2int );
			rulelist.add( rule );
		}
		br.close();
		for( Integer token : str2int.values() ) {
			Rule rule = new Rule( token, token );
			rulelist.add( rule );
		}
		return new Rule_ACAutomata( rulelist );
	}

	public void genString( int avgLength, int nRecords, File file, double equivratio, Rule_ACAutomata atm ) throws IOException {
		HashSet<Record> records = new HashSet<Record>();
		int count = 0;
		while( records.size() < nRecords ) {
			if( random.nextDouble() < equivratio ) {
				while( true ) {
					Record rec = randomString( avgLength );
					Record equivrecord = rec.randomTransform( atm, random ).randomTransform( atm, random );
					if( equivrecord.compareTo( rec ) != 0 ) {
						records.add( rec );
						records.add( equivrecord );
						++count;
						break;
					}
				}
			}
			else {
				Record rec = randomString( avgLength );
				records.add( rec );
			}
		}
		BufferedWriter bw = new BufferedWriter( new FileWriter( file ) );
		for( Record rec : records ) {
			bw.write( rec.toString( int2str ) );
			bw.newLine();
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
		return tokens;
	}

	// Rules: do not follow zipf distribution. Use uniform distribution
	public void genSkewRule( int lhsmax, int rhsmax, int nRules, File file ) throws IOException {
		HashSet<Rule> rules = new HashSet<Rule>();

		// generate rule
		while( rules.size() < nRules ) {
			// 1. sample length of lhs and rhs
			int lhslen = random.nextInt( lhsmax ) + 1;
			int rhslen = random.nextInt( rhsmax ) + 1;
			// 2. generate random lhs
			int[] from = random( lhslen );
			int[] to = random( rhslen );

			if( lhslen == rhslen ) {
				boolean equals = true;
				for( int t = 0; t < lhslen; ++t ) {
					if( from[ t ] != to[ t ] ) {
						equals = false;
						break;
					}
				}

				if( equals ) {
					continue;
				}
			}

			Rule rule = new Rule( from, to );
			rules.add( rule );
		}

		// To file
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

		RuleInfo info = new RuleInfo();
		info.setSynthetic( lhsmax, rhsmax, nRules, seed, nTokens, zipf );
		info.saveToFile( file.getName() + "_info.json" );
	}

	public void genUniformRule( int lhsmax, int rhsmax, int nRules, File file ) throws IOException {
		HashSet<Rule> rules = new HashSet<Rule>();
		while( rules.size() < nRules ) {
			// 1. sample length of lhs and rhs
			int lhslen = random.nextInt( lhsmax ) + 1;
			int rhslen = random.nextInt( rhsmax ) + 1;
			int[] from = new int[ lhslen ];
			int[] to = new int[ rhslen ];
			Set<Integer> samples = new HashSet<Integer>();
			// 2. generate random lhs
			while( samples.size() < lhslen ) {
				int token = random.nextInt( int2str.size() );
				if( samples.contains( token ) )
					continue;
				from[ samples.size() ] = token;
				samples.add( token );
			}
			samples.clear();
			// 2. generate random rhs
			while( samples.size() < rhslen ) {
				int token = random.nextInt( int2str.size() );
				if( samples.contains( token ) )
					continue;
				to[ samples.size() ] = token;
				samples.add( token );
			}
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
