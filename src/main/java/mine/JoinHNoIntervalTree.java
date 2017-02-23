package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tools.Algorithm;
import tools.IntIntRecordTriple;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Parameters;
import tools.Rule;
import tools.RuleTrie;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinHNoIntervalTree extends Algorithm {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static boolean singleside = false;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	static String outputfile;

	static Validator checker;
	/**
	 * Key: (token, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	WYK_HashMap<IntegerPair, List<IntIntRecordTriple>> idx;

	protected JoinHNoIntervalTree( String rulefile, String Rfile, String Sfile ) throws IOException {
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
		WYK_HashMap<IntegerPair, Integer> invokes = new WYK_HashMap<IntegerPair, Integer>();
		for( Record rec : tableS ) {
			IntegerSet[] availableTokens = rec.getAvailableTokens();
			int searchmax = Math.min( availableTokens.length, maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				for( int token : availableTokens[ i ] ) {
					IntegerPair ip = new IntegerPair( token, i );
					Integer count = invokes.get( ip );
					if( count == null )
						count = 1;
					else
						count += 1;
					invokes.put( ip, count );
				}
			}
		}

		idx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>();
		for( Record rec : tableR ) {
			IntegerSet[] availableTokens = rec.getAvailableTokens();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				int invoke = 0;
				for( int token : availableTokens[ i ] ) {
					IntegerPair ip = new IntegerPair( token, i );
					Integer count = invokes.get( ip );
					if( count != null )
						invoke += count;
				}
				if( invoke < minInvokes ) {
					minIdx = i;
					minInvokes = invoke;
				}
			}

			predictCount += minInvokes;

			for( int token : availableTokens[ minIdx ] ) {
				IntegerPair ip = new IntegerPair( token, minIdx );
				List<IntIntRecordTriple> list = idx.get( ip );
				if( list == null ) {
					list = new ArrayList<IntIntRecordTriple>();
					idx.put( ip, list );
				}
				list.add( new IntIntRecordTriple( range[ 0 ], range[ 1 ], rec ) );
			}
			elements += availableTokens[ minIdx ].size();
		}
		System.out.println( "Predict : " + predictCount );
		System.out.println( "Idx size : " + elements );

		///// Statistics
		int sum = 0;
		long count = 0;
		for( List<IntIntRecordTriple> list : idx.values() ) {
			if( list.size() == 1 )
				continue;
			sum++;
			count += list.size();
		}
		System.out.println( "iIdx size : " + count );
		System.out.println( "Rec per idx : " + ( (double) count ) / sum );
	}

	private WYK_HashSet<IntegerPair> join() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

		long appliedRules_sum = 0;
		for( Record recS : tableS ) {
			IntegerSet[] availableTokens = recS.getAvailableTokens();
			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			int searchmax = Math.min( availableTokens.length, maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				List<List<Record>> candidatesList = new ArrayList<List<Record>>();
				for( int token : availableTokens[ i ] ) {
					IntegerPair ip = new IntegerPair( token, i );
					List<IntIntRecordTriple> tree = idx.get( ip );

					if( tree == null )
						continue;
					List<Record> list = new ArrayList<Record>();
					for( IntIntRecordTriple e : tree )
						if( StaticFunctions.overlap( e.min, e.max, range[ 0 ], range[ 1 ] ) )
							list.add( e.rec );
					candidatesList.add( list );
				}
				List<Record> candidates = StaticFunctions.union( candidatesList, idComparator );
				if( skipChecking )
					continue;
				for( Record recR : candidates ) {
					int compare = checker.isEqual( recR, recS );
					if( compare >= 0 ) {
						rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
						appliedRules_sum += compare;
					}
				}
			}
		}
		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );

		return rslt;
	}

	private void buildIndexSingleSide() {
		long elements = 0;
		long predictCount = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		WYK_HashMap<IntegerPair, Integer> invokes = new WYK_HashMap<IntegerPair, Integer>();
		for( Record rec : tableS ) {
			int[] tokens = rec.getTokenArray();
			for( int i = 0; i < tokens.length; ++i ) {
				IntegerPair ip = new IntegerPair( tokens[ i ], i );
				Integer count = invokes.get( ip );
				if( count == null )
					count = 1;
				else
					count += 1;
				invokes.put( ip, count );
			}
		}

		idx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>();
		for( Record rec : tableR ) {
			IntegerSet[] availableTokens = rec.getAvailableTokens();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				int invoke = 0;
				for( int token : availableTokens[ i ] ) {
					IntegerPair ip = new IntegerPair( token, i );
					Integer count = invokes.get( ip );
					if( count != null )
						invoke += count;
				}
				if( invoke < minInvokes ) {
					minIdx = i;
					minInvokes = invoke;
				}
			}

			predictCount += minInvokes;

			for( int token : availableTokens[ minIdx ] ) {
				IntegerPair ip = new IntegerPair( token, minIdx );
				List<IntIntRecordTriple> list = idx.get( ip );
				if( list == null ) {
					list = new ArrayList<IntIntRecordTriple>();
					idx.put( ip, list );
				}
				list.add( new IntIntRecordTriple( range[ 0 ], range[ 1 ], rec ) );
			}
			elements += availableTokens[ minIdx ].size();
		}
		System.out.println( "Predict : " + predictCount );
		System.out.println( "Idx size : " + elements );

		///// Statistics
		int sum = 0;
		long count = 0;
		for( List<IntIntRecordTriple> list : idx.values() ) {
			if( list.size() == 1 )
				continue;
			sum++;
			count += list.size();
		}
		System.out.println( "iIdx size : " + count );
		System.out.println( "Rec per idx : " + ( (double) count ) / sum );
	}

	private WYK_HashSet<IntegerPair> joinSingleSide() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

		long appliedRules_sum = 0;
		for( Record recS : tableS ) {
			int[] tokens = recS.getTokenArray();
			int minlength = recS.getMinLength();
			int maxlength = recS.getMaxLength();
			for( int i = 0; i < tokens.length; ++i ) {
				List<Record> candidatesList = new ArrayList<Record>();
				IntegerPair ip = new IntegerPair( tokens[ i ], i );
				List<IntIntRecordTriple> tree = idx.get( ip );

				if( tree == null )
					continue;
				for( IntIntRecordTriple e : tree )
					if( StaticFunctions.overlap( e.min, e.max, minlength, maxlength ) )
						candidatesList.add( e.rec );
				if( skipChecking )
					continue;
				for( Record recR : candidatesList ) {
					int compare = checker.isEqual( recR, recS );
					if( compare >= 0 ) {
						rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
						appliedRules_sum += compare;
					}
				}
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
		if( singleside )
			buildIndexSingleSide();
		else
			buildIndex();
		System.out.print( "Building Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		startTime = System.currentTimeMillis();
		WYK_HashSet<IntegerPair> rslt = ( singleside ? joinSingleSide() : join() );
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

		long startTime = System.currentTimeMillis();
		JoinHNoIntervalTree inst = new JoinHNoIntervalTree( Rulefile, Rfile, Sfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run();

		Validator.printStats();
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run( String[] args ) {
		// TODO Auto-generated method stub
		
	}
}
