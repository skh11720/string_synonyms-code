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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class Generator2 extends Generator {
	
	static final int maxLhs = 2;
	static final int maxRhs = 2;

	ObjectArrayList<int[]> lhsList = null;
	static Object2IntOpenHashMap<int[]> lhsFreqMap = null;
//	Object2ObjectOpenHashMap<int[], ObjectArrayList<Rule>> lhs2rulesMap = null;

	public Generator2( int nDistinctTokens, double zipf, long seed ) {
		super( nDistinctTokens, zipf, seed );
		// TODO Auto-generated constructor stub
	}

	protected static String getRuleFilePath( int nToken, int maxLhs, int maxRhs, int nRule, double skewR, long seed ) {
//		return nToken + "_" + maxLhs + "_" + maxRhs + "_" + nRule + "_" + skewR + "_" + seed;
		return nToken + "_" + nRule + "_" + skewR + "_" + seed;
	}

	protected static String getDataFilePath( int nToken, int avgRecLen, int nRecord, int avgNAR, double SEL, double skewP, double equivratio, long seed ) {
//		return nToken + "_" + avgRecLen + "_" + nRecord + "_" + avgNAR + "_" + skewZ + "_" + skewP + "_" + equivratio + "_" + seed;
		return nToken + "_" + nRecord + "_" + avgNAR + "_" + SEL + "_" + skewP + "_" + seed;
	}

	public static String generateRules( int nToken, int nRule, int LCF, double skewZ, double skewR, long seed, String outputPath ) throws IOException {
		if (!(new File(outputPath+"/rule")).isDirectory()) (new File(outputPath+"/rule")).mkdirs();
		String storePath = outputPath + "/rule/" + getRuleFilePath( nToken, maxLhs, maxRhs, nRule, skewR, seed );
//		System.out.println( storePath );
		Generator2 gen = new Generator2( nToken, skewZ, seed );
		gen.genRule( maxLhs, maxRhs, nRule, LCF, skewR, seed, storePath + ".txt" );

		RuleInfo info = new RuleInfo();
		info.setSynthetic( maxLhs, maxRhs, nRule, seed, nToken, skewZ );
		info.saveToFile( storePath + "_rule_info.json" );
		return storePath+".txt";
	}

	public static String generateRecords( int nToken, int avgRecLen, int avgNAR, int nRecord, double skewZ, double skewP, double equivratio, long seed, String outputPath, String rulefile ) throws IOException  {
		if (!(new File(outputPath+"/data")).isDirectory()) (new File(outputPath+"/data")).mkdirs();
		String storePath = outputPath + "/data/" + getDataFilePath( nToken, avgRecLen, nRecord, avgNAR, skewZ, skewP, equivratio, seed );
//		System.out.println( storePath );
		Generator2 gen = new Generator2( nToken, skewZ, seed );
		ACAutomataR atm = gen.readRules( rulefile );
//		ACAutomataR atm = new ACAutomataR( gen.rulelist );
		gen.genString( avgRecLen, avgNAR, nRecord, skewP, storePath + ".txt", equivratio, atm );

		DataInfo info = new DataInfo();
		info.setSynthetic( avgRecLen, nRecord, seed, nToken, skewZ, equivratio );
		info.saveToFile( storePath + "_data_info.json" );
		return storePath+".txt";
	}

	public void genRule( int lhsmax, int rhsmax, int nRules, int lhsFactor, double skewR, long seed, String filename ) throws IOException {
		random.setSeed( seed0 ); // re-initialize the randomizer
		HashSet<Rule> rules = new HashSet<Rule>();
		BufferedWriter bw = new BufferedWriter( new FileWriter( filename ) );
		
		// build LHS zipfian distribution of size nRules
		Set<int[]> lhsSet = new ObjectOpenHashSet<>();
		while ( lhsSet.size() < lhsFactor*tokenRatio.length ) lhsSet.add( randomArrayWithMaxLen( maxLhs ) );
		List<int[]> lhsCandList = new ObjectArrayList<>( lhsSet );
		double[] lhsRatio = new double[lhsSet.size()];
		for ( int j=0; j<lhsRatio.length; ++j ) {
			lhsRatio[j] = 1.0/Math.pow( j+1, skewR );
			if ( j!= 0 ) lhsRatio[j] += lhsRatio[j-1];

		}
		for ( int j=0; j<lhsRatio.length; ++j ) lhsRatio[j] /= lhsRatio[ lhsRatio.length-1 ];
		Random random = new Random(seed);
		
		while ( rules.size() < nRules ) {
			// 1. sample lhs from the zipfian dist
			double rd = random.nextDouble();
			int lhsIdx = Arrays.binarySearch( lhsRatio, rd );
			if( lhsIdx < 0 ) lhsIdx = -lhsIdx - 1;
			int[] lhs = lhsCandList.get( lhsIdx );
			
			// 2. generate rhs
			Rule rule = null;
			while (true) {
				int[] rhs = randomArrayWithMaxLen( maxRhs );
				if ( Arrays.equals( lhs, rhs ) ) continue; // ignore the self rules
				
				// 3 produce a rule
				rule = new Rule( lhs, rhs );
				if ( rules.contains( rule ) ) continue; // avoid duplications
				else break;
			}
			rules.add( rule );
			for( int from : rule.getLeft() )
				bw.write( from + " " );
			bw.write( ", " );
			for( int to : rule.getRight() )
				bw.write( to + " " );
			bw.newLine();
		}
		bw.close();
		rulelist = new ObjectArrayList<>(rules);
		
		// build the inverted index and the lhsList
//		lhs2rulesMap = new Object2ObjectOpenHashMap<>();
//		lhs2rulesMap.defaultReturnValue( new ObjectArrayList<>() );
//		for ( Rule rule : rules ) lhs2rulesMap.get( rule.getLeft() ).add( rule );
		lhsFreqMap = new Object2IntOpenHashMap<>();
		lhsFreqMap.defaultReturnValue(0);
		lhsList = new ObjectArrayList<>();
		for ( Rule rule : rules ) {
			lhsFreqMap.addTo( rule.getLeft(), 1 );
//			System.out.println( rule );
		}
		for ( Entry<int[], Integer> entry : lhsFreqMap.entrySet() ) {
			int[] lhs = entry.getKey();
			int count = entry.getValue().intValue();
			if ( count > 0 ) lhsList.add( lhs );
		}
//		lhsList = new ObjectArrayList<>( lhsFreqMap.keySet() );
		
		// DEBUG
//		System.out.println( "nRule: "+rules.size() );
//		System.out.println( "LHS frequency" );
//		System.out.println( Arrays.toString( lhsFreqMap.values().stream().sorted((x,y)->-Integer.compare(x,y)).mapToInt( Integer::intValue ).limit( 20 ).toArray() ) );
//		System.out.println( "LHS histogram" );
//		Int2IntOpenHashMap hist = new Int2IntOpenHashMap();
//		hist.defaultReturnValue( 0 );
//		lhsFreqMap.values().stream().forEach( val -> { hist.addTo( val, 1 ); });
//		for ( int i=0; i<10; ++i ) System.out.println( "hihst["+i+"]: "+hist.get( i ) );
//		System.out.println( "freq sum: "+ lhsFreqMap.values().stream().reduce( (x, y )->x+y ).get().intValue() );
//		System.out.println( "hist sum: "+ hist.values().stream().reduce( (x,y)->x+y ).get().intValue() );
	}

	protected void genString( int avgLength, int avgNAR, int nRecords, double skewP, String fileName, double equivratio, ACAutomataR atm ) throws IOException {
		random.setSeed( seed0 ); // re-initialize the randomizer
		HashSet<Record> records = new HashSet<Record>();
		int count = 0;
		while( records.size() < nRecords ) {
			if( random.nextDouble() < equivratio ) {
				while( true ) {
					// make sure there exists equivalent records in the data set
					Record rec = randomString( avgLength, avgNAR, skewP );
//					Record equivrecord = randomTransform( randomTransform( rec, atm, random ), atm, random ); // bidirectional case
					Record equivrecord = randomTransform( rec, atm, random ); // unidirectional case
					if( equivrecord.compareTo( rec ) != 0 ) {
						records.add( rec );
						records.add( equivrecord );
						++count;
					}
					break;
				}
			}
			else {
				Record rec = randomString( avgLength, avgNAR, skewP );
				records.add( rec );
				// DEBUG
//				rec.preprocessRules( atm );
//				System.out.println( rec.getApplicableRules(0).length+", "+rec );
			}
		}
		BufferedWriter bw = new BufferedWriter( new FileWriter( fileName ) );
		for( Record rec : records ) {
			bw.write( rec.toString() );
			bw.newLine();
		}
		bw.close();
//		System.out.println( "[genString] equiv count: " + count );
	}

	protected Record randomString( int avgLength, int avgNAR, double skewP ) {
		// skewP is the probability of sampling tokens from the zipfian dist of LHSs

		if ( avgNAR >= 0 ) {
			int len = (int) Math.max( 1, avgLength + random.nextGaussian() );
			int target_nar = (int) Math.max( 0, avgNAR + random.nextGaussian() );
			IntArrayList tokenList = new IntArrayList();
			for ( int nar=0; nar<target_nar; ) {
				int[] lhs = null;
				if ( nar ==0 && random.nextDouble() < skewP ) { // sample from the zipfian dist of LHSs, only the first lhs
					lhs = rulelist.get( random.nextInt( rulelist.size() ) ).getLeft();
				}
				else {
//					if ( random.nextDouble() < 0.5 ) 
					lhs = lhsList.get( random.nextInt( lhsList.size() ) );
//					else lhs = new int[] {sampleToken()};
				}
				for ( int token : lhs ) tokenList.add( token );
//				System.out.println( Arrays.toString( lhs )+"\t"+lhsFreqMap.getInt( lhs ) );
				nar += lhsFreqMap.getInt( lhs );
			} // end for nar
			return new Record( tokenList.toIntArray() );
		}
		else {
			// 1. sample length of string
			int len = (int) Math.max( 1, avgLength + random.nextGaussian() );
			// 2. generate random string
			int[] tokens = random( len );
			// System.out.println( Arrays.toString( tokens ) );
			Record rec = new Record( tokens );
			// System.out.println( "Rec: " + rec.toString() );
			return rec;
		}
	}
	
	protected int[] randomArrayWithMaxLen( int maxLen ) {
		int len = random.nextInt( maxLen ) + 1;
		int[] rarr = new int[len];
		for ( int i=0; i<len; ++i ) rarr[i] = random.nextInt( tokenRatio.length ); // uniform sampling
		return rarr;
	}
	
	protected int sampleToken() {
		int token = Arrays.binarySearch( tokenRatio, random.nextDouble() );
		if( token < 0 ) token = -token - 1;
		return token;
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
		// build the inverted index and the lhsList
//		lhs2rulesMap = new Object2ObjectOpenHashMap<>();
//		lhs2rulesMap.defaultReturnValue( new ObjectArrayList<>() );
//		for ( Rule rule : rules ) lhs2rulesMap.get( rule.getLeft() ).add( rule );
		lhsFreqMap = new Object2IntOpenHashMap<>();
		lhsFreqMap.defaultReturnValue(0);
		lhsList = new ObjectArrayList<>();
		for ( Rule rule : rulelist ) {
			lhsFreqMap.addTo( rule.getLeft(), 1 );
//			System.out.println( rule );
		}
		for ( Entry<int[], Integer> entry : lhsFreqMap.entrySet() ) {
			int[] lhs = entry.getKey();
			int count = entry.getValue().intValue();
			if ( count > 0 ) lhsList.add( lhs );
		}
		return new ACAutomataR( rulelist );
	}

}
