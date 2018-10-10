package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class Generator2 extends Generator {

	public Generator2( int nDistinctTokens, double zipf, long seed ) {
		super( nDistinctTokens, zipf, seed );
		// TODO Auto-generated constructor stub
	}

	protected static String getDataFilePath( int nToken, int avgRecLen, int nRecord, int avgNAR, double skewZ, double equivratio, long seed ) {
		return nToken + "_" + avgRecLen + "_" + nRecord + "_" + avgNAR + "_" + skewZ + "_" + equivratio + "_" + seed;
	}

	public static String generateRules( int nToken, int maxLhs, int maxRhs, int nRule, double skewZ, long seed, String outputPath ) throws IOException {
		if (!(new File(outputPath+"/rule")).isDirectory()) (new File(outputPath+"/rule")).mkdirs();
		String storePath = outputPath + "/rule/" + getRuleFilePath( nToken, maxLhs, maxRhs, nRule, skewZ, seed );
		System.out.println( storePath );
		Generator gen = new Generator( nToken, skewZ, seed );
		gen.genSkewRule( maxLhs, maxRhs, nRule, storePath + ".txt" );

		RuleInfo info = new RuleInfo();
		info.setSynthetic( maxLhs, maxRhs, nRule, seed, nToken, skewZ );
		info.saveToFile( storePath + "_rule_info.json" );
		return storePath+".txt";
	}

	public static void generateRecords( int nToken, int avgRecLen, int avgNAR, int nRecord, double skewZ, double equivratio, long seed, String outputPath, String rulefile ) throws IOException  {
		if (!(new File(outputPath+"/data")).isDirectory()) (new File(outputPath+"/data")).mkdirs();
		String storePath = outputPath + "/data/" + getDataFilePath( nToken, avgRecLen, nRecord, avgNAR, skewZ, equivratio, seed );
		System.out.println( storePath );
		Generator2 gen = new Generator2( nToken, skewZ, seed );
		ACAutomataR atm = null;

		if( equivratio != 0 ) {
			atm = gen.readRules( rulefile );
		}
		gen.genString( avgRecLen, avgNAR, nRecord, storePath + ".txt", equivratio, atm );

		DataInfo info = new DataInfo();
		info.setSynthetic( avgRecLen, nRecord, seed, nToken, skewZ, equivratio );
		info.saveToFile( storePath + "_data_info.json" );
	}

	protected void genString( int avgLength, int avgNAR, int nRecords, String fileName, double equivratio, ACAutomataR atm )
			throws IOException {
		HashSet<Record> records = new HashSet<Record>();
		int count = 0;
		while( records.size() < nRecords ) {
			if( random.nextDouble() < equivratio ) {
				while( true ) {
					// make sure there exists equivalent records in the data set
					Record rec = randomString( avgLength, avgNAR );
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
				Record rec = randomString( avgLength, avgNAR );
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

	protected Record randomString( int avgLength, int avgNAR ) {
		if ( avgLength > 0 && avgNAR == -1 ) {
			// 1. sample length of string
			int len = (int) Math.max( 1, avgLength + random.nextGaussian() );
			// 2. generate random string
			int[] tokens = random( len );
			// System.out.println( Arrays.toString( tokens ) );
			Record rec = new Record( tokens );
			// System.out.println( "Rec: " + rec.toString() );
			return rec;
		}
		else if ( avgLength == -1 && avgNAR >= 0 ) {
			int nar = (int) Math.max( 0, avgNAR + Math.sqrt( avgNAR )*random.nextGaussian() );
			IntArrayList tokenList = new IntArrayList();
			for ( int i=0; i<nar; ++i ) {
				Rule rule = rulelist.get( random.nextInt( rulelist.size() ) );
				for ( int token : rule.getLeft() ) tokenList.add( token );
			}
			return new Record( tokenList.toIntArray() );
		}
		return null;
	}
}
