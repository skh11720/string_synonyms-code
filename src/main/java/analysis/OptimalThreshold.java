package analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mine.JoinH2GramNoIntervalTree;
import mine.Naive1;
import mine.Record;
import tools.Algorithm;
import tools.IntegerPair;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import validator.TopDownHashSetSinglePath_DS;
import validator.Validator;

public class OptimalThreshold extends Algorithm {
	Rule_ACAutomata ruleatm;
	RuleTrie trie;

	protected OptimalThreshold( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		ruleatm = new Rule_ACAutomata( rulelist );
		trie = new RuleTrie( rulelist );
		Record.setRuleTrie( trie );
	}

	public static void main( String[] args ) throws IOException {
		OptimalThreshold inst = new OptimalThreshold( args[ 1 ], args[ 0 ], args[ 0 ] );
		System.out.println( "Start" );
		if( args[ 2 ].compareTo( "-a" ) == 0 )
			inst.measureAlpha();
		else if( args[ 2 ].compareTo( "-b" ) == 0 )
			inst.measureBeta();
		else if( args[ 2 ].compareTo( "-c" ) == 0 )
			inst.measureGamma();
		else if( args[ 2 ].compareTo( "-d" ) == 0 )
			inst.measureDelta();
		else if( args[ 2 ].compareTo( "-e" ) == 0 ) {
			double ratio = Double.parseDouble( args[ 3 ] );
			boolean skipequiv = args.length == 5;
			inst.estimateJoinMH( ratio, skipequiv );
		}
		else if( args[ 2 ].compareTo( "-f" ) == 0 ) {
			double ratio = Double.parseDouble( args[ 3 ] );
			int threshold = Integer.parseInt( args[ 4 ] );
			boolean skipequiv = args.length == 6;
			inst.estimateNaive( ratio, threshold, skipequiv );
		}
		else {
			printUsage();
			System.exit( 0 );
		}
	}

	private static void printUsage() {
		System.out.println( "Usage : [Data] [Rule] [Flag]" );
		System.out.println( "Flag  : -a : estimate alpha" );
		System.out.println( "        -b : estimate beta" );
		System.out.println( "        -c : estimate gamma" );
		System.out.println( "        -d : estimate delta" );
	}

	private void measureAlpha() {
		long expandedrecs = 0;
		long expandedsize = 0;
		long totalexpandedrec = 0;
		long cost = 0;
		long sizesum = 0;
		List<Record> expandcandidates = new ArrayList<Record>();
		for( Record rec : tableR ) {
			rec.preprocessRules( ruleatm, false );
			rec.preprocessSuffixApplicableRules();
			rec.preprocessEstimatedRecords();
			long size = rec.getEstNumRecords();
			if( size <= 1E5 ) {
				expandcandidates.add( rec );
				List<Record> expanded = rec.expandAll( trie );
				expandedrecs += expanded.size();
				for( Record exprec : expanded )
					expandedsize += exprec.size();
				expandedsize -= rec.size();
				cost += rec.getEstExpandCost();
				sizesum += rec.size();
				try {
					assert ( size >= expanded.size() );
				}
				catch( java.lang.AssertionError e ) {
					e.printStackTrace();
					System.err.println( rec.toString( strlist ) );
					System.exit( 1 );
				}
			}
			totalexpandedrec += size;
		}
		long starttime = System.nanoTime();
		for( Record rec : expandcandidates ) {
			rec.expandAll( trie );
		}
		long duration = System.nanoTime() - starttime;

		System.out.println( "Total " + totalexpandedrec + " exp recs" );
		System.out.println( "Total(<\\theta) " + expandedrecs + " exp recs" );
		System.out.println( "Total est " + cost + " cost" );
		System.out.println( "Total " + expandedsize + " cost" );
		System.out.println( "Total " + sizesum + " sizesum" );
		System.out.println( duration + "ns" );
		System.out.println( "Avg time/cost " + ( duration / cost ) + " ns" );
		System.out.println( "Avg time/exprecs " + ( duration / expandedrecs ) + " ns" );
	}

	private void measureBeta() {
		long expandedrecs = 0;
		long expandedsize = 0;
		long totalexpandedrec = 0;
		long cost = 0;
		long sizesum = 0;
		List<Record> expandcandidates = new ArrayList<Record>();
		for( Record rec : tableR ) {
			rec.preprocessRules( ruleatm, false );
			rec.preprocessSuffixApplicableRules();
			rec.preprocessEstimatedRecords();
			if( rec.getEstNumRecords() <= 1E4 ) {
				expandcandidates.add( rec );
				List<Record> expanded = rec.expandAll( trie );
				expandedrecs += expanded.size();
				for( Record exprec : expanded )
					expandedsize += exprec.size();
				expandedsize -= rec.size();
				cost += rec.getEstExpandCost();
				sizesum += rec.size();
			}
			totalexpandedrec += rec.getEstNumRecords();
		}
		long starttime = System.nanoTime();
		Map<Record, List<Record>> map = new HashMap<Record, List<Record>>();
		for( Record rec : expandcandidates ) {
			List<Record> expanded = rec.expandAll( trie );
			for( Record erec : expanded ) {
				List<Record> list = map.get( erec );
				if( list == null ) {
					list = new ArrayList<Record>();
					map.put( erec, list );
				}
				list.add( rec );
			}
		}
		long duration = System.nanoTime() - starttime;

		System.out.println( "Total " + totalexpandedrec + " exp recs" );
		System.out.println( "Total(<\\theta) " + expandedrecs + " exp recs" );
		System.out.println( "Total est " + cost + " cost" );
		System.out.println( "Total " + expandedsize + " cost" );
		System.out.println( "Total " + sizesum + " sizesum" );
		System.out.println( duration + "ns" );
		System.out.println( "Avg time/cost " + ( duration / cost ) + " ns" );
		System.out.println( "Avg time/exprecs " + ( duration / expandedrecs ) + " ns" );

		starttime = System.nanoTime();
		for( Record rec : expandcandidates ) {
			List<Record> expanded = rec.expandAll( trie );
			for( Record erec : expanded )
				map.get( erec );
		}
		duration = System.nanoTime() - starttime;

		System.out.println( duration + "ns" );
		System.out.println( "Avg time/cost " + ( duration / cost ) + " ns" );
		System.out.println( "Avg time/exprecs " + ( duration / expandedrecs ) + " ns" );
	}

	private void measureGamma() {
		long candsum = 0;
		long candsummin = 0;
		List<Record> expandcandidates = new ArrayList<Record>();
		for( Record rec : tableR ) {
			rec.preprocessRules( ruleatm, false );
			rec.preprocessLengths();
			rec.preprocessSuffixApplicableRules();
			rec.preprocessEstimatedRecords();
			if( rec.getEstNumRecords() <= 1E4 )
				expandcandidates.add( rec );
		}
		long starttime = System.nanoTime();
		Map<Integer, Map<IntegerPair, Integer>> counter = new HashMap<Integer, Map<IntegerPair, Integer>>();
		for( Record rec : expandcandidates ) {
			List<Set<IntegerPair>> twograms = rec.get2Grams();
			for( int i = 0; i < twograms.size(); ++i ) {
				Set<IntegerPair> twograms_i = twograms.get( i );
				Map<IntegerPair, Integer> counter_i = counter.get( i );
				if( counter_i == null ) {
					counter_i = new HashMap<IntegerPair, Integer>();
					counter.put( i, counter_i );
				}
				candsum += twograms_i.size();
				if( i < rec.getMinLength() )
					candsummin += twograms_i.size();
				for( IntegerPair twogram : twograms_i ) {
					Integer c = counter_i.get( twogram );
					if( c == null )
						counter_i.put( twogram, 1 );
					else
						counter_i.put( twogram, 1 + c );
				}
			}
		}
		long duration = System.nanoTime() - starttime;

		System.out.println( "Total " + candsum + " signatures" );
		System.out.println( "Min " + candsummin + " signatures" );
		System.out.println( duration + "ns" );
		System.out.println( "Avg time/sig " + ( duration / candsum ) + " ns" );
	}

	private void measureDelta() {
		JoinH2GramNoIntervalTree inst = new JoinH2GramNoIntervalTree( this );
		JoinH2GramNoIntervalTree.checker = new TopDownHashSetSinglePath_DS();
		JoinH2GramNoIntervalTree.skipChecking = true;
		JoinH2GramNoIntervalTree.outputfile = "null";
		long starttime = System.nanoTime();
		inst.run();
		long duration = System.nanoTime() - starttime;
		JoinH2GramNoIntervalTree.skipChecking = false;
		long starttime2 = System.nanoTime();
		inst.run();
		long duration2 = System.nanoTime() - starttime2;

		double delta = ( duration2 - duration ) / Validator.checked;
		System.out.println( "Total " + Validator.checked + " pairs checked" );
		System.out.println( "Exec time " + ( duration2 - duration ) + " ns" );
		System.out.println( "Estimated delta = " + delta );
	}

	private void estimateJoinMH( double sampleratio, boolean skipequiv ) {
		List<Record> tableR = new ArrayList<Record>();
		List<Record> tableS = new ArrayList<Record>();
		for( Record r : this.tableR )
			if( Math.random() < sampleratio )
				tableR.add( r );
		for( Record s : this.tableS )
			if( Math.random() < sampleratio )
				tableS.add( s );
		this.tableR = tableR;
		this.tableS = tableR;

		JoinH2GramNoIntervalTree inst = new JoinH2GramNoIntervalTree( this );
		JoinH2GramNoIntervalTree.skipChecking = skipequiv;
		JoinH2GramNoIntervalTree.checker = new TopDownHashSetSinglePath_DS();
		JoinH2GramNoIntervalTree.outputfile = "null";
		try {
			inst.run();
		}
		catch( Exception e ) {
		}
		Validator.printStats();

		System.out.println( "Orig build Idx time : " + inst.buildIndexTime );
		System.out.println( "Orig join time : " + inst.joinTime );
	}

	private void estimateNaive( double sampleratio, int threshold, boolean skipequiv ) {
		List<Record> tableR = new ArrayList<Record>();
		List<Record> tableS = new ArrayList<Record>();
		for( Record r : this.tableR )
			if( Math.random() < sampleratio )
				tableR.add( r );
		for( Record s : this.tableS )
			if( Math.random() < sampleratio )
				tableS.add( s );
		this.tableR = tableR;
		this.tableS = tableR;

		Naive1 inst = new Naive1( this );
		Naive1.threshold = threshold;
		Naive1.skipequiv = skipequiv;
		try {
			inst.run();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
		Validator.printStats();

		System.out.println( "Orig build Idx time : " + inst.buildIndexTime );
		System.out.println( "Orig join time : " + inst.joinTime );

		measureAlpha();
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "OptimalThreshold";
	}

	@Override
	public void run( String[] args ) {
		// TODO Auto-generated method stub
	}
}
