package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generate strings and rules based on zipf distribution.
 * The orders of tokens for both data sets are exactly the same.
 * Manually generate equivalent strings.
 */

public class Generator {
	protected double[] tokenRatio;
	protected Random random;
	protected TokenIndex tokenIndex;
	protected List<Rule> rulelist;

	public Generator( int nDistinctTokens, double zipf, long seed ) {
		tokenRatio = new double[ nDistinctTokens ];
		for( int i = 0; i < tokenRatio.length; ++i ) {
			tokenRatio[ i ] = 1.0 / Math.pow( i + 1, zipf );
			if( i != 0 )
				tokenRatio[ i ] += tokenRatio[ i - 1 ];
		}
		for( int i = 0; i < tokenRatio.length; ++i )
			tokenRatio[ i ] /= tokenRatio[ tokenRatio.length - 1 ];
		random = new Random( seed );
		tokenIndex = new TokenIndex();
		tokenIndex.getID( "" );
	}

	protected static void printUsage() {
		System.out.println(
				"-d <rulefile> <#tokens> <#avg length> <#records> <skewness> <equiv ratio> <random seed> <output path>: generate data with given rulefile" );
		System.out.println( "-r <#tokens> <max lhs len> <max rhs len> <#rules> <random seed>  <output path>: generate rule" );
		System.exit( 1 );
	}

	protected static String getRuleFilePath( int nToken, int maxLhs, int maxRhs, int nRule, double skewZ, long seed ) {
		return nToken + "_" + maxLhs + "_" + maxRhs + "_" + nRule + "_" + skewZ + "_" + seed;
	}

	protected static String getDataFilePath( int nToken, int avgRecLen, int nRecord, double skewZ, double equivratio, long seed ) {
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
			generateRules( nToken, maxLhs, maxRhs, nRule, skewZ, seed, outputPath );
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
			generateRecords( nToken, avgRecLen, nRecord, skewZ, equivratio, seed, outputPath, rulefile );

		}
		else {
			printUsage();
		}
	}
	
	public static String generateRules( int nToken, int maxLhs, int maxRhs, int nRule, double skewZ, long seed, String outputPath ) throws IOException {
		if (!(new File(outputPath+"/rule")).isDirectory()) (new File(outputPath+"/rule")).mkdirs();
		String storePath = outputPath + "/rule/" + getRuleFilePath( nToken, maxLhs, maxRhs, nRule, skewZ, seed );
		Generator gen = new Generator( nToken, skewZ, seed );
		gen.genSkewRule( maxLhs, maxRhs, nRule, storePath + ".txt" );

		RuleInfo info = new RuleInfo();
		info.setSynthetic( maxLhs, maxRhs, nRule, seed, nToken, skewZ );
		info.saveToFile( storePath + "_rule_info.json" );
		return storePath+".txt";
	}
	
	public static void generateRecords( int nToken, int avgRecLen, int nRecord, double skewZ, double equivratio, long seed, String outputPath, String rulefile ) throws IOException  {
		if (!(new File(outputPath+"/data")).isDirectory()) (new File(outputPath+"/data")).mkdirs();
		String storePath = outputPath + "/data/" + getDataFilePath( nToken, avgRecLen, nRecord, skewZ, equivratio, seed );
		Generator gen = new Generator( nToken, skewZ, seed );
		ACAutomataR atm = null;

		if( equivratio != 0 ) {
			atm = gen.readRules( rulefile );
		}
		gen.genString( avgRecLen, nRecord, storePath + ".txt", equivratio, atm );

		DataInfo info = new DataInfo();
		info.setSynthetic( avgRecLen, nRecord, seed, nToken, skewZ, equivratio );
		info.saveToFile( storePath + "_data_info.json" );
	}

	public ACAutomataR readRules( String rulefile ) throws IOException {
//		List<Rule> rulelist = new ArrayList<Rule>();
		rulelist = new ArrayList<Rule>();
		BufferedReader br = new BufferedReader( new FileReader( rulefile ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			Rule rule = new Rule( line, tokenIndex );
			rulelist.add( rule );
		}
		br.close();
		for( Integer token : tokenIndex.token2IntMap.values() ) {
			Rule rule = new Rule( token, token );
			rulelist.add( rule );
		}
		return new ACAutomataR( rulelist );
	}

	protected void genString( int avgLength, int nRecords, String fileName, double equivratio, ACAutomataR atm )
			throws IOException {
		HashSet<Record> records = new HashSet<Record>();
		int count = 0;
		while( records.size() < nRecords ) {
			if( random.nextDouble() < equivratio ) {
				while( true ) {
					// make sure there exists equivalent records in the data set
					Record rec = randomString( avgLength );
//					Record equivrecord = randomTransform( randomTransform( rec, atm, random ), atm, random ); // bidirectional case
					Record equivrecord = randomTransform( rec, atm, random ); // unidirectional case
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
			bw.write( rec.toString() );
			bw.newLine();
		}
		bw.close();
		System.out.println( "[genString] equiv count: " + count );
	}

	protected Record randomString( int avgLength ) {
		// 1. sample length of string
		int len = (int) Math.max( 1, avgLength + random.nextGaussian() );
		// 2. generate random string
		int[] tokens = random( len );
		// System.out.println( Arrays.toString( tokens ) );
		Record rec = new Record( tokens );
		// System.out.println( "Rec: " + rec.toString() );
		return rec;
	}

	// generate random string
	protected int[] random( int len ) {
		Set<Integer> samples = new HashSet<Integer>();
		int[] tokens = new int[ len ];
		// 2. generate random string
		while( samples.size() < len ) {
			double rd = random.nextDouble();
			int token = Arrays.binarySearch( tokenRatio, rd );
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
				for( int from : rule.getLeft() )
					bw.write( from + " " );
				bw.write( ", " );
				for( int to : rule.getRight() )
					bw.write( to + " " );
				bw.newLine();
			}
		}
		bw.close();
	}

	public void genSkewRuleAbbr( int lhsmax, int rhsmax, int nRules, String filename ) throws IOException {
		HashSet<Rule> rules = new HashSet<Rule>();

		// To file
		BufferedWriter bw = new BufferedWriter( new FileWriter( filename ) );

		// generate rule
		while( rules.size() < nRules ) {
			int lhslen = random.nextInt( lhsmax ) + 1;
			int[] lhs = random( lhslen );

			for ( int i=0; i<10; i++ ) {
				int rhslen = random.nextInt( rhsmax ) + 1;
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
						--i;
						continue;
					}
				}

				Rule rule = new Rule( lhs, rhs );
				boolean added = rules.add( rule );

				if( added ) {
					for( int from : rule.getLeft() )
						bw.write( from + " " );
					bw.write( ", " );
					for( int to : rule.getRight() )
						bw.write( to + " " );
					bw.newLine();
				}
				else --i;
				
				if ( rules.size() >= nRules ) break;
			}
		}
		bw.close();
	}

	public Record randomTransform( Record rec, ACAutomataR atm, Random rand ) {
		List<Integer> list = new ArrayList<Integer>();
		Rule[][] rules = atm.applicableRules( rec.getTokensArray() );
		int idx = 0;
		while( idx < rec.getTokenCount() ) {
			int ruleidx = rand.nextInt( rules[ idx ].length );
			Rule rule = rules[ idx ][ ruleidx ];
			for( int token : rule.getRight() ) {
				list.add( token );
			}
			idx += rule.getLeft().length;
		}
		int[] transformed = new int[ list.size() ];
		for( idx = 0; idx < list.size(); ++idx )
			transformed[ idx ] = list.get( idx );
		return new Record( transformed );
	}
}
