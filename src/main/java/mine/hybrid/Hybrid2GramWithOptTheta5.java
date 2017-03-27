package mine.hybrid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mine.JoinH2GramNoIntervalTree2;
import mine.Naive1;
import mine.Record;
import mine.RecordIDComparator;
import mine.RecordPair;
import snu.kdd.synonym.tools.StatContainer;
import tools.Algorithm;
import tools.Parameters;
import tools.Rule;
import tools.RuleTrie;
import validator.Validator;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 * Utilize a very simple method to estimate the best theta.
 * It first sample data and run both naive and joinmin algorithm.
 * Then, compare the execution times to estimate the best theta.
 */
public class Hybrid2GramWithOptTheta5 extends Algorithm {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static boolean singleside;
	static Validator checker;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	static String outputfile;

	int joinThreshold = -1;

	private static final int RECORD_CLASS_BYTES = 64;

	/* private int intarrbytes(int len) {
	 * // Accurate bytes in 64bit machine is:
	 * // ceil(4 * len / 8) * 8 + 16
	 * return len * 4 + 16;
	 * } */

	long maxtheta;

	protected Hybrid2GramWithOptTheta5( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
	}

	/**
	 * If theta == -1, find the maximum theta as well.
	 * Else, use the given theta.
	 */
	private void estNaiveExecTime() {
		Runtime runtime = Runtime.getRuntime();
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );
		long memlimit = runtime.freeMemory() / 2;

		long currexpanded = 0;
		long memcost = 0;
		for( Record s : tableT ) {
			long expanded = s.getEstNumRecords();
			if( expanded != currexpanded )
				currexpanded = expanded;
			memcost += memcost( s.getEstExpandCost(), expanded );
			if( memcost > memlimit )
				break;
		}
		if( memcost > memlimit )
			maxtheta = currexpanded - 1;
		else
			maxtheta = currexpanded;

		currexpanded = 0;
		memcost = 0;
		for( Record t : tableS ) {
			long expanded = t.getEstNumRecords();
			if( expanded != currexpanded )
				currexpanded = expanded;
			memcost += memcost( t.getEstExpandCost(), expanded );
			if( memcost > memlimit )
				break;
		}
		if( memcost > memlimit )
			maxtheta = Math.max( maxtheta, currexpanded - 1 );
		else
			maxtheta = Math.max( maxtheta, currexpanded );
	}

	private int memcost( long lengthsum, long exps ) {
		int memcost = 0;
		// Size for the integer arrays
		memcost += 4 * lengthsum + 16 * exps;
		// Size for the Record instance
		memcost += RECORD_CLASS_BYTES * exps;
		// Pointers in the inverted index
		memcost += 8 * exps;
		// Pointers in the Hashmap (in worst case)
		// Our hashmap filling ratio is 0.5: 24 / 0.5 = 48
		memcost += 48 * exps;

		return memcost;
	}

	public void statistics() {
		long strlengthsum = 0;
		long strmaxinvsearchrangesum = 0;
		int strs = 0;
		int maxstrlength = 0;

		long rhslengthsum = 0;
		int rules = 0;
		int maxrhslength = 0;

		for( Record rec : tableT ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}
		for( Record rec : tableS ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}

		for( Rule rule : rulelist ) {
			int length = rule.getTo().length;
			++rules;
			rhslengthsum += length;
			maxrhslength = Math.max( maxrhslength, length );
		}

		System.out.println( "Average str length: " + strlengthsum + "/" + strs );
		System.out.println( "Average maxinvsearchrange: " + strmaxinvsearchrangesum + "/" + strs );
		System.out.println( "Maximum str length: " + maxstrlength );
		System.out.println( "Average rhs length: " + rhslengthsum + "/" + rules );
		System.out.println( "Maximum rhs length: " + maxrhslength );
	}

	@Override
	protected void preprocess( boolean compact, int maxIndex, boolean useAutomata ) {
		super.preprocess( compact, maxIndex, useAutomata );

		// Sort R and S with expanded sizes
		Comparator<Record> cmp = new Comparator<Record>() {
			@Override
			public int compare( Record o1, Record o2 ) {
				long est1 = o1.getEstNumRecords();
				long est2 = o2.getEstNumRecords();
				return Long.compare( est1, est2 );
			}
		};
		Collections.sort( tableT, cmp );
		Collections.sort( tableS, cmp );

		// Reassign ID
		long maxSEstNumRecords = 0;
		long maxTEstNumRecords = 0;
		for( int i = 0; i < tableT.size(); ++i ) {
			Record s = tableT.get( i );
			s.setID( i );
			maxSEstNumRecords = Math.max( maxSEstNumRecords, s.getEstNumRecords() );
		}
		for( int i = 0; i < tableS.size(); ++i ) {
			Record t = tableS.get( i );
			t.setID( i );
			maxTEstNumRecords = Math.max( maxTEstNumRecords, t.getEstNumRecords() );
		}

		System.out.println( "Max S expanded size : " + maxSEstNumRecords );
		System.out.println( "Max T expanded size : " + maxTEstNumRecords );
	}

	public void run( double sampleratio ) {
		long startTime = System.currentTimeMillis();
		preprocess( compact, maxIndex, useAutomata );
		System.out.print( "Preprocess finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		// Retrieve statistics
		statistics();

		startTime = System.currentTimeMillis();
		// Compute the maximum theta
		estNaiveExecTime();
		// checkLongestIndex();
		System.out.print( "estNaiveExecTime finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) + "ms" );

		// Modify index to get optimal theta
		startTime = System.currentTimeMillis();
		findTheta( sampleratio );
		System.out.print( "Estimation finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) + "ms" );
	}

	private void findTheta( double sampleratio ) {
		// Find the best threshold
		long starttime = System.nanoTime();
		// exclusive
		long maxtheta = this.maxtheta + 1;
		// inclusive
		long mintheta = 1;

		final int trials = 1;

		List<Record> oS = tableT;
		// List<Record> oT = tableS;
		tableT = new ArrayList<Record>();
		tableS = tableT;
		// tableS = new ArrayList<Record>();
		Runtime rt = Runtime.getRuntime();

		while( mintheta < maxtheta - 1 ) {
			long currtheta = ( maxtheta + mintheta ) / 2;
			// Self join
			sampleList( oS, tableT, currtheta, sampleratio );
			// sampleList(oT, tableS, currtheta, sampleratio);
			/* try {
			 * StaticFunctions.write2file(tableT, "sample1");
			 * StaticFunctions.write2file(tableS, "sample2");
			 * } catch (Exception e) {
			 * } */

			long naivetime = Long.MAX_VALUE;
			long joinmintime = Long.MAX_VALUE;
			for( int i = 0; i < trials; ++i ) {
				System.out.println( "Free mem : " + rt.freeMemory() );
				// Do naive join
				Naive1 naiveinst = new Naive1( this );
				List<RecordPair> naive = naiveinst.runWithoutPreprocess();
				naivetime = Math.min( naivetime, naiveinst.buildIndexTime + naiveinst.joinTime );
				naiveinst.clearIndex();
				naiveinst = null;
				System.gc();

				// Do JoinMin
				JoinH2GramNoIntervalTree2 joinmininst = new JoinH2GramNoIntervalTree2( this );
				JoinH2GramNoIntervalTree2.checker = checker;
				List<RecordPair> joinmin = joinmininst.runWithoutPreprocess();
				joinmintime = Math.min( joinmintime, joinmininst.buildIndexTime + joinmininst.joinTime );
				joinmininst.clearIndex();
				joinmininst = null;
				System.gc();

				// Compare two results
				assert ( naive.size() == joinmin.size() );
				for( int j = 0; j < naive.size(); ++j ) {
					RecordPair rp1 = naive.get( j );
					RecordPair rp2 = joinmin.get( j );
					if( rp1.record1 != rp2.record1 || rp1.record2 != rp2.record2 ) {
						System.out.println( "Error(2) : Two results do not match" );
						System.out.println( "Naive : " + rp1.record1 + " == " + rp1.record2 );
						System.out.println( "JoinMin : " + rp2.record1 + " == " + rp2.record2 );
						System.exit( 1 );
					}
				}
			}

			System.out.println( "theta = " + currtheta + " : N " + naivetime + " / JM " + joinmintime );
			if( joinmintime > naivetime )
				mintheta = currtheta;
			else
				maxtheta = currtheta;
		}
		System.out.println( "Final theta : " + mintheta );
		long duration = System.nanoTime() - starttime;
		System.out.println( "Estimated in " + duration + " ns" );
	}

	/**
	 * Find the maximum index which expands less or equal to 'theta' records.
	 */
	private int findMaxIdx( List<Record> list, long theta ) {
		int min = 0;
		int max = list.size();
		while( min < max - 1 ) {
			int curr = ( max + min ) / 2;
			Record rec = list.get( curr );
			if( rec.getEstNumRecords() > theta )
				max = curr;
			else
				min = curr;
		}
		return min;
	}

	/**
	 * Update sS and sT using oS and oT. Maximum number of expanded records in oS
	 * and oT are changed from 'from' to 'to'.
	 */
	private void sampleList( List<Record> oS, List<Record> sS, long theta, double sampleratio ) {
		sS.clear();
		Set<Integer> sample = new HashSet<Integer>();
		int sidx = findMaxIdx( oS, theta );
		int size = (int) ( sidx * sampleratio );
		while( sample.size() < size ) {
			int rnd = (int) ( Math.random() * sidx );
			sample.add( rnd );
		}
		for( Integer idx : sample ) {
			Record rec = oS.get( idx );
			assert ( rec.getEstNumRecords() <= theta );
			sS.add( oS.get( idx ) );
		}
		final Comparator<Record> comp = new RecordIDComparator();
		Collections.sort( sS, comp );
	}

	public static void main( String[] args ) throws IOException {
		Parameters params = Parameters.parseArgs( args );
		String Rfile = params.getInputX();
		String Sfile = params.getInputY();
		String Rulefile = params.getInputRules();
		outputfile = params.getOutput();

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		maxIndex = params.getMaxIndex();
		compact = params.isCompact();
		singleside = params.isSingleside();
		checker = params.getValidator();

		long startTime = System.currentTimeMillis();
		Hybrid2GramWithOptTheta5 inst = new Hybrid2GramWithOptTheta5( Rulefile, Rfile, Sfile );
		inst.joinThreshold = params.getJoinThreshold();
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run( params.getSampleRatio() );
		Validator.printStats();
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "Hybrid2GramWithOptTheta5";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		// TODO Auto-generated method stub

	}
}