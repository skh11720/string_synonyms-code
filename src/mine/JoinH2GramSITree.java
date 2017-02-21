package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.Algorithm;
import tools.IntegerPair;
import tools.Parameters;
import tools.Rule;
import tools.RuleTrie;
import tools.WYK_HashMap;
import validator.Validator;

public class JoinH2GramSITree extends Algorithm {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static boolean exact2grams = false;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	static String outputfile;

	static Validator checker;
	Map<Integer, SI_Tree_JoinH<IntegerPair>> idx;

	protected JoinH2GramSITree( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		int size = -1;

		readRules( rulefile );
		Record.setStrList( strlist );
		tableR = readRecords( Rfile, size );
		tableS = readRecords( Sfile, size );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
		Record.setRuleTrie( ruletrie );
	}

	private void buildIndex() {
		long elements = 0;
		long predictCount = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		Map<Integer, Map<IntegerPair, Integer>> invokes = new HashMap<Integer, Map<IntegerPair, Integer>>();
		// Map<LongIntPair, Integer> invokes = new HashMap<LongIntPair, Integer>();
		for( Record rec : tableS ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2Grams();
			int searchmax = Math.min( available2Grams.size(), maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, Integer> curridx_invokes = invokes.get( i );
				if( curridx_invokes == null ) {
					curridx_invokes = new WYK_HashMap<IntegerPair, Integer>();
					invokes.put( i, curridx_invokes );
				}
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					Integer count = curridx_invokes.get( twogram );
					if( count == null )
						count = 1;
					else
						count += 1;
					curridx_invokes.put( twogram, count );
				}
			}
		}

		idx = new WYK_HashMap<Integer, SI_Tree_JoinH<IntegerPair>>();
		for( Record rec : tableR ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2Grams();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				if( available2Grams.get( i ).isEmpty() )
					continue;
				int invoke = 0;
				Map<IntegerPair, Integer> curridx_invokes = invokes.get( i );
				// There is no invocation count: this is the minimum point
				if( curridx_invokes == null ) {
					minIdx = i;
					minInvokes = 0;
					break;
				}
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					Integer count = curridx_invokes.get( twogram );
					if( count != null )
						invoke += count;
				}
				if( invoke < minInvokes ) {
					minIdx = i;
					minInvokes = invoke;
				}
			}

			predictCount += minInvokes;

			SI_Tree_JoinH<IntegerPair> curridx = idx.get( minIdx );
			if( curridx == null ) {
				curridx = new SI_Tree_JoinH<IntegerPair>( checker );
				idx.put( minIdx, curridx );
			}
			curridx.add( rec, available2Grams.get( minIdx ) );
			elements += available2Grams.get( minIdx ).size();
		}
		System.out.println( "Predict : " + predictCount );
		System.out.println( "Idx size : " + elements );
	}

	private List<IntegerPair> join() {
		List<IntegerPair> rslt = new ArrayList<IntegerPair>();

		long appliedRules_sum = 0;
		for( Record recS : tableS ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? recS.getExact2Grams() : recS.get2Grams();
			int searchmax = Math.min( available2Grams.size(), maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				SI_Tree_JoinH<IntegerPair> curridx = idx.get( i );
				if( curridx == null )
					continue;
				List<Record> search = curridx.search( recS, available2Grams.get( i ), skipChecking );
				for( Record r : search )
					rslt.add( new IntegerPair( r.getID(), recS.getID() ) );
			}
		}
		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );

		return rslt;
	}

	public void statistics() {
		long strlengthsum = 0;
		long strmaxinvsearchrangesum = 0;
		int strs = 0;
		int maxstrlength = 0;

		long rhslengthsum = 0;
		int rules = 0;
		int maxrhslength = 0;

		for( Record rec : tableR ) {
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

	public void run() {
		long startTime = System.currentTimeMillis();
		preprocess( compact, maxIndex, useAutomata );
		System.out.print( "Preprocess finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		// Retrieve statistics
		statistics();

		startTime = System.currentTimeMillis();
		buildIndex();
		System.out.print( "Building Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		startTime = System.currentTimeMillis();
		Collection<IntegerPair> rslt = join();
		System.out.print( "Join finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( rslt.size() );

		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			for( IntegerPair ip : rslt ) {
				if( ip.i1 != ip.i2 )
					bw.write(
							tableR.get( ip.i1 ).toString( strlist ) + "\t==\t" + tableR.get( ip.i2 ).toString( strlist ) + "\n" );
			}
			bw.close();
		}
		catch( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		compact = params.isCompact();
		checker = params.getValidator();
		exact2grams = params.isExact2Grams();

		long startTime = System.currentTimeMillis();
		JoinH2GramSITree inst = new JoinH2GramSITree( Rulefile, Rfile, Sfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run();

		Validator.printStats();
	}
}
