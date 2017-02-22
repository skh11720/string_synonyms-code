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

	public Generator( int nDistinctTokens, double zipf, long seed ) {
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
				"-d <rulefile> <#tokens> <#avg length> <#records> <skewness> <equiv ratio> <random seed> <output path>: generate data with given rulefile" );
		System.out.println( "-r <#tokens> <max lhs len> <max rhs len> <#rules> <random seed>  <output path>: generate rule" );
		System.exit( 1 );
	}

	public static String getRuleFilePath( int nToken, int maxLhs, int maxRhs, int nRule, double skewZ, long seed ) {
		return nToken + "_" + maxLhs + "_" + maxRhs + "_" + nRule + "_" + skewZ + "_" + seed;
	}

	public static String getDataFilePath( int nToken, int avgRecLen, int nRecord, double skewZ, double equivratio, long seed ) {
		return nToken + "_" + avgRecLen + "_" + nRecord + "_" + skewZ + "_" + equivratio + "_" + seed;
	}

	public static void main( String[] args ) throws IOException {
		// build rule first and then generate data
		if( args[ 0 ].equals( "-r" ) || args[ 0 ].equals( "-cr" ) ) {
			int nToken = Integer.parseInt( args[ 1 ] );
			int maxLhs = Integer.parseInt( args[ 2 ] );
			int maxRhs = Integer.parseInt( args[ 3 ] );
			int nRule = Integer.parseInt( args[ 4 ] );
			double skewZ = Double.parseDouble( args[ 5 ] );
			long seed = Long.parseLong( args[ 6 ] );
			String outputPath = args[ 7 ];

			String storePath = outputPath + "/rule/" + getRuleFilePath( nToken, maxLhs, maxRhs, nRule, skewZ, seed );

			if( args[ 0 ].equals( "-cr" ) ) {
				System.out.println( storePath );
			}
			else {
				// if args[ 0 ].equals( "-r" )
				// generate data
				new File( storePath ).mkdirs();

				Generator gen = new Generator( nToken, skewZ, seed );
				gen.genSkewRule( maxLhs, maxRhs, nRule, storePath + "/rule.txt" );

				RuleInfo info = new RuleInfo();
				info.setSynthetic( maxLhs, maxRhs, nRule, seed, nToken, skewZ );
				info.saveToFile( storePath + "/rule_info.json" );
			}
		}
		else if( args[ 0 ].equals( "-d" ) || args[ 0 ].equals( "-cd" ) ) {
			int nToken = Integer.parseInt( args[ 1 ] );
			int avgRecLen = Integer.parseInt( args[ 2 ] );
			int nRecord = Integer.parseInt( args[ 3 ] );
			double skewZ = Double.parseDouble( args[ 4 ] );
			double equivratio = Double.parseDouble( args[ 5 ] );
			long seed = Long.parseLong( args[ 6 ] );
			String outputPath = args[ 7 ];
			String rulefile = null;

			String storePath = outputPath + "/data/" + getDataFilePath( nToken, avgRecLen, nRecord, skewZ, equivratio, seed );

			if( args[ 0 ].equals( "-cd" ) ) {
				System.out.println( storePath );
			}
			else {
				new File( storePath ).mkdirs();

				Generator gen = new Generator( nToken, skewZ, seed );
				Rule_ACAutomata atm = null;

				// TODO: support when equivration != 0
				if( equivratio != 0 ) {
					rulefile = args[ 8 ];
					atm = gen.readRules( rulefile );
				}
				gen.genString( avgRecLen, nRecord, storePath + "/data.txt", equivratio, atm );

				DataInfo info = new DataInfo();
				info.setSynthetic( avgRecLen, nRecord, seed, nToken, skewZ, equivratio );
				info.saveToFile( storePath + "/data_info.json" );
			}
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

	public void genString( int avgLength, int nRecords, String fileName, double equivratio, Rule_ACAutomata atm )
			throws IOException {
		HashSet<Record> records = new HashSet<Record>();
		int count = 0;
		while( records.size() < nRecords ) {
			if( random.nextDouble() < equivratio ) {
				while( true ) {
					// make sure there exists equivalent records in the data set
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
		BufferedWriter bw = new BufferedWriter( new FileWriter( fileName ) );
		for( Record rec : records ) {
			bw.write( rec.toString( int2str ) );
			bw.newLine();
		}
		bw.close();
		System.out.println( "[genString] equiv count: " + count );
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
	public void genSkewRule( int lhsmax, int rhsmax, int nRules, String filename ) throws IOException {
		HashSet<Rule> rules = new HashSet<Rule>();

		// To file
		BufferedWriter bw = new BufferedWriter( new FileWriter( filename ) );

		// generate rule
		while( rules.size() < nRules ) {
			// 1. sample length of lhs and rhs
			int lhslen = random.nextInt( lhsmax ) + 1;
			int rhslen = random.nextInt( rhsmax ) + 1;
			// 2. generate random lhs
			int[] lhs = random( lhslen );
			int[] rhs = random( rhslen );

			if( lhslen == rhslen ) {
				boolean equals = true;
				for( int t = 0; t < lhslen; ++t ) {
					if( lhs[ t ] != rhs[ t ] ) {
						equals = false;
						break;
					}
				}

				if( equals ) {
					continue;
				}
			}

			Rule rule = new Rule( lhs, rhs );
			boolean added = rules.add( rule );

			if( added ) {
				for( int from : rule.getFrom() )
					bw.write( from + " " );
				bw.write( ", " );
				for( int to : rule.getTo() )
					bw.write( to + " " );
				bw.newLine();
			}
		}
		bw.close();
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
