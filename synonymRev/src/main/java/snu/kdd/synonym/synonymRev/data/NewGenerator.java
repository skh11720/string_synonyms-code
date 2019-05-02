package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * NewGenerator
 * 
 * generate synthetic datasets.
 */

public class NewGenerator {
	
	private static final int maxLhs = 2;
	private static final int maxRhs = 2;
	private static final int avgLen = 10;

	private final String dataHomeTmpl; // args: nToken, skewD
	private final String rulePathTmpl; // args: nToken, nRule, skewD
	private final String dataPathTmpl; // args: nToken, nRule, ANR, skewD, SEL, nRecord
	private final long seed;
	
	private int nToken;
	private int nRecord;
	private int nRule;
	private int ANR;
	private double skewD;
	private double SEL;
	private Random random;
	private String dataHome;

	private List<Rule> ruleList = null;
	private Object2IntOpenHashMap<int[]> lhsFreqMap = null;
	private ObjectArrayList<int[]> lhsList = null;
	private Int2ObjectOpenHashMap<List<int[]>> freq2lhsMap = null;
	private int maxFreq;
	private ACAutomataR atm = null;

	private double[] tokenDist;
	private TokenIndex tokenIndex;
	
//	public static void generate( String datadir, int nToken, int nRecord, int nRule, int ANR, double skewD, double SEL, long seed ) {
//		NewGenerator gen = new NewGenerator(nToken, nRecord, nRule, ANR, skewD, SEL, seed);
//		try { gen.generate(datadir); }
//		catch ( IOException e ) { e.printStackTrace(); }
//	}

	public NewGenerator( String dataHomeTmpl, String rulePathTmpl, String dataPathTmpl, long seed ) {
		this.dataHomeTmpl = dataHomeTmpl;
		this.rulePathTmpl = rulePathTmpl;
		this.dataPathTmpl = dataPathTmpl;
		this.seed = seed;
	}
	
//	private void generate( String datadir ) throws IOException {
//		buildDict();
//		generateRules( datadir );
//		System.out.println("rule generation finished");
//		generateRecords( datadir );
//		System.out.println("record generation finished");
//	}

	public void buildDict( int nToken, double skewD ) {
		this.nToken = nToken;
		this.skewD = skewD;
		dataHome = String.format( dataHomeTmpl, nToken, skewD );
		tokenDist = new double[nToken];
		tokenIndex = new TokenIndex();
		for( int i=0; i<tokenDist.length; ++i ) {
			tokenDist[i] = 1.0 / Math.pow( i+1, skewD );
			if( i!=0 )
				tokenDist[i] += tokenDist[i-1];
		}
		for( int i=0; i < tokenDist.length; ++i )
			tokenDist[i] /= tokenDist[tokenDist.length-1];
		tokenIndex.getID( "" );
	}

	// Rules: do not follow zipf distribution. Use uniform distribution
	/*
	 * Return the writePath of the generated rules.
	 */
//	String datadir = parentPath + syn_id + String.format("_D%d_K%.2f", nToken, skewD );
	public String generateRules( int nRule ) throws IOException {
		ruleList = new ArrayList<Rule>();
		lhsFreqMap = new Object2IntOpenHashMap<>();
		lhsList = new ObjectArrayList<>();
		freq2lhsMap = new Int2ObjectOpenHashMap<>();
		random = new Random(seed);
		this.nRule = nRule;
		if (!(new File(dataHome+"/rule")).isDirectory()) (new File(dataHome+"/rule")).mkdirs();

		while( ruleList.size() < nRule ) {
			int lhslen = random.nextInt( maxLhs ) + 1;
			int rhslen = random.nextInt( maxRhs ) + 1;
			int[] lhs = random( lhslen );
			int[] rhs = random( rhslen );

			if( lhslen == rhslen && Arrays.equals( lhs, rhs ) ) continue; // avoid generating self rules

			Rule rule = new Rule( lhs, rhs );
			ruleList.add( rule );
		}

		// collect statistics
		for ( Rule rule : ruleList ) lhsFreqMap.addTo( rule.getLeft(), 1 );
		for ( Entry<int[], Integer> entry : lhsFreqMap.entrySet() ) {
			int[] lhs = entry.getKey();
			int freq = entry.getValue();
			lhsList.add( lhs );
			if ( !freq2lhsMap.containsKey(freq) ) freq2lhsMap.put( freq, new ObjectArrayList<int[]>() );
			freq2lhsMap.get(freq).add(lhs);
		}
		maxFreq = freq2lhsMap.keySet().stream().max(Integer::compare).get();
		atm = new ACAutomataR( ruleList );

		// write the rules into disk
		String writePath = dataHome + String.format( rulePathTmpl, nToken, nRule, skewD );
		BufferedWriter bw = new BufferedWriter( new FileWriter( writePath ) );
		for ( Rule rule : ruleList ) {
			for( int from : rule.getLeft() ) bw.write( from + " " );
			bw.write( ", " );
			for( int to : rule.getRight() ) bw.write( to + " " );
			bw.newLine();
		}
		bw.close();
//		RuleInfo info = new RuleInfo();
//		info.setSynthetic( maxLhs, maxRhs, nRule, seed, nToken, skewZ );
//		info.saveToFile( storePath + "_rule_info.json" );
		
		return writePath;
	}

	/*
	 * Return the writePath of the generated records.
	 */
	public String generateRecords( int nRecord, int ANR, double SEL ) throws IOException  {
		this.nRecord = nRecord;
		this.ANR = ANR;
		this.SEL = SEL;
		random = new Random(seed);

		if (!(new File(dataHome+"/data")).isDirectory()) (new File(dataHome+"/data")).mkdirs();
		String writePath = dataHome + String.format( dataPathTmpl, nToken, nRule, ANR, skewD, SEL, nRecord );
		PrintWriter pw = new PrintWriter( new BufferedWriter( new FileWriter( writePath ) ) );
		
		int recordCount = 0;
//		int equivCount = 0;
		while( recordCount < nRecord ) {
			if ( random.nextDouble() < SEL && recordCount < nRecord-1 ) {
				for ( int i=0;; ++i ) {
					// make sure there exists equivalent records in the data set
					Record rec = randomString();
					Record equivrecord = randomTransform( rec, atm ); // unidirectional case
					if( equivrecord.compareTo( rec ) != 0 ) {
						pw.println( rec.toString() );
						pw.println( equivrecord.toString() );
//						++equivCount;
						recordCount += 2;
						break;
					}
					if ( i >= 1000000 ) throw new RuntimeException("failed to generate equivalent record pairs.");
				}
			}
			else {
				pw.println( randomString().toString() );
				++recordCount;
			}
		}
		pw.close();

//		DataInfo info = new DataInfo();
//		info.setSynthetic( avgRecLen, nRecord, seed, nToken, skewZ, equivratio );
//		info.saveToFile( writePath + "_data_info.json" );
		
		return writePath;
	}
	
	private Record randomString() {
//		return randomStringLen();
		return randomStringANR();
	}

	private Record randomStringLen() {
		// 1. sample length of string
		int len = (int) Math.max( 1, avgLen + random.nextGaussian() );
		// 2. generate random string
		int[] tokens = random( len );
		// System.out.println( Arrays.toString( tokens ) );
		Record rec = new Record( tokens );
		// System.out.println( "Rec: " + rec.toString() );
		return rec;
	}
	
	private Record randomStringANR() {
		IntArrayList tokens = new IntArrayList();
		int anr0 = (int) Math.max( 1, ANR + random.nextGaussian() );
		for ( int anr=0, len=0; anr<anr0; ) {
			int freq = random.nextInt(anr0 - anr + 1); // range from 0 to anr0-anr
			int[] lhs = null;
			if ( freq == 0 ) {
				Rule rule = ruleList.get( random.nextInt(ruleList.size()) );
				lhs = rule.getLeft();
			}
			else {
				freq = Math.min( freq, maxFreq );
				List<int[]> ruleListFreq = freq2lhsMap.get(freq);
				lhs = ruleListFreq.get( random.nextInt(ruleListFreq.size()) );
			}
			for ( int token : lhs ) tokens.add(token);
			len += lhs.length;
			anr += lhsFreqMap.getInt(lhs);
			if ( len > 2*avgLen ) break;
		}
		return new Record(tokens.toIntArray());
	}

	// generate random string
	private int[] random( int len ) {
		Set<Integer> samples = new HashSet<Integer>();
		int[] tokens = new int[ len ];
		// 2. generate random string
		while( samples.size() < len ) {
			double rd = random.nextDouble();
			int token = Arrays.binarySearch( tokenDist, rd );
			if( token < 0 )
				token = -token - 1;
			if( samples.contains( token ) )
				continue;
			tokens[ samples.size() ] = token;
			samples.add( token );
		}
		return tokens;
	}

	public Record randomTransform( Record rec, ACAutomataR atm ) {
		List<Integer> list = new ArrayList<Integer>();
		Rule[][] rules = atm.applicableRules( rec.getTokensArray() );
		int idx = 0;
		while( idx < rec.getTokenCount() ) {
			int ruleidx = random.nextInt( rules[idx].length );
			Rule rule = rules[idx][ruleidx];
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

//	private int sampleToken() {
//		int token = Arrays.binarySearch( tokenDist, random.nextDouble() );
//		if( token < 0 ) token = -token - 1;
//		return token;
//	}
	
	private static void printUsage() {
//		System.out.println(
//				"-d <rulefile> <#tokens> <#avg length> <#records> <skewness> <equiv ratio> <random seed> <output path>: generate data with given rulefile" );
//		System.out.println( "-r <#tokens> <max lhs len> <max rhs len> <#rules> <random seed>  <output path>: generate rule" );
//		System.exit( 1 );
	}

	public static void main( String[] args ) throws IOException {
		args = "-D 100 -N 200 -R 500 -M 13 -K 0.1 -S 0.1".split(" ");
		
		Options opts = new Options();
		opts.addOption( new Option("D", "dict_size", true, "dictionary size") );
		opts.addOption( new Option("N", "num_rec", true, "number of records") );
		opts.addOption( new Option("R", "num_rule", true, "number of rules") );
		opts.addOption( new Option("M", "anr", true, "average number of applicable rules per string") );
		opts.addOption( new Option("K", "skewD", true, "skewness of dictionary") );
		opts.addOption( new Option("S", "sel", true, "selectivity") );
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse( opts, args );
			int nToken = Integer.parseInt( cmd.getOptionValue("D") );
			int nRecord = Integer.parseInt( cmd.getOptionValue("N") );
			int nRule = Integer.parseInt( cmd.getOptionValue("R") );
			int ANR = Integer.parseInt( cmd.getOptionValue("M") );
			double skewD = Double.parseDouble( cmd.getOptionValue("K") );
			double SEL = Double.parseDouble( cmd.getOptionValue("S") );
			long seed = 0;
			String dataHomeTmpl = "SYN_D%d_K%.2f";
			String rulePathTmpl = "/rule/D%d_R%d_K%.2f.txt";
			String dataPathTmpl = "/data/D%d_R%d_M%d_K%.2f_S%.2e_N%d.txt";

			NewGenerator gen = new NewGenerator(dataHomeTmpl, rulePathTmpl, dataPathTmpl, seed);
			long ts = System.nanoTime();
			gen.buildDict(nToken, skewD);
			long afterBuildDictTime = System.nanoTime();
			gen.generateRules(nRule);
			long afterGenRuleTime = System.nanoTime();
			gen.generateRecords(nRecord, ANR, SEL);
			long afterGenRecrodTime = System.nanoTime();
			
			System.out.println("buildDictTime: "+(afterBuildDictTime-ts)/1e6);
			System.out.println("genRuleTime: "+(afterGenRuleTime-afterBuildDictTime)/1e6);
			System.out.println("genRecordTime: "+(afterGenRecrodTime-afterGenRuleTime)/1e6);
		}
		catch ( ParseException e ) { e.getMessage(); }
	}
}
