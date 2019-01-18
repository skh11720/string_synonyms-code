package snu.kdd.synonym.synonymRev.index;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class NaiveIndex extends AbstractIndex {
	private WYK_HashMap<Record, ArrayList<Integer>> idx;
	protected final boolean isSelfJoin;
	protected final String filenamePrefix = "NaiveIndex";

	public long indexTime = 0;
	public long joinTime = 0;
	public long indexingTime = 0;
	public long expandTime = 0;
	public long searchTime = 0;

	public long idxsize = 0;
	public double totalExp = 0;
	public double totalExpLength = 0;
	public int skippedCount = 0;
	
	public long sumTransLenS = 0;
	public long sumLenT = 0;

	public double alpha;
	public double beta;

	public NaiveIndex( Query query, StatContainer stat, boolean addStat, double avgTransformed ) {
		isSelfJoin = query.selfJoin;

		final long starttime = System.nanoTime();
		int initialSize = (int) ( query.indexedSet.size() * avgTransformed / 2 );
		if ( initialSize > 10000 ) initialSize = 10000;
		if ( initialSize < 10 ) initialSize = 10;

		idx = new WYK_HashMap<Record, ArrayList<Integer>>( initialSize );

		for( int i = 0; i < query.indexedSet.size(); ++i ) {
			final Record recR = query.indexedSet.getRecord( i );

			long expandStartTime;
			if( DEBUG.NaiveON ) {
				expandStartTime = System.nanoTime();
			}

			if( DEBUG.NaiveON ) {
				expandTime += System.nanoTime() - expandStartTime;
			}

			long indexingStartTime = System.nanoTime();

			totalExpLength += recR.getTokenCount();
			addExpaneded( recR, i );

			indexingTime += System.nanoTime() - indexingStartTime;
			sumLenT += recR.size();
		}

		indexTime = System.nanoTime() - starttime;
		if( totalExpLength == 0 ) totalExpLength = 1;
		alpha = indexTime / totalExpLength;
	}

	protected void addExpaneded( Record expanded, int recordId ) {
		ArrayList<Integer> list = idx.get( expanded );

		if( list == null ) {
			// new expression
			list = new ArrayList<Integer>( 5 );
			if( expanded.getID() == -1 ) {
				idx.putNonExist( expanded, list );
			}
			else {
				idx.putNonExist( new Record( expanded.getTokensArray() ), list );
			}
		}

		// If current list already contains current record as the last element, skip adding
		if( !list.isEmpty() && list.get( list.size() - 1 ) == recordId ) {
			return;
		}

		list.add( recordId );
	}

	@Override
	public void joinOneRecord( Record recS, Set<IntegerPair> rslt, Validator checker ) {
		/*
		 * NOTE: checker is not used.
		 */
		long expandStartTime = System.nanoTime();
		final List<Record> expanded = recS.expandAll();
		expandTime += System.nanoTime() - expandStartTime;

		totalExp += expanded.size();

		final Set<Integer> candidates = new HashSet<Integer>();

		long searchStartTime = System.nanoTime();
		for( final Record exp : expanded ) {

			final List<Integer> overlapidx = idx.get( exp );
			sumTransLenS += exp.size();

			if( overlapidx == null ) {
				continue;
			}

			for( Integer i : overlapidx ) {
				candidates.add( i );
			}
			
		}
		for( final Integer idx : candidates ) {
//			rslt.add( new IntegerPair( recS.getID(), idx ) );
			AlgorithmTemplate.addSeqResult( recS, idx, rslt, isSelfJoin );
		}

		searchTime += System.nanoTime() - searchStartTime;
	}
	
	@Override
	protected void postprocessAfterJoin( StatContainer stat ) {
		beta = joinTime / totalExp;

//		stat.add( "Join_Naive_Result", rslt.size() );

		if( DEBUG.NaiveON ) {
			stat.add( "Est_Join_1_expandTime", expandTime );
			stat.add( "Est_Join_2_searchTime", searchTime );
			stat.add( "Est_Join_3_totalTime", Double.toString( joinTime ) );

			Runtime runtime = Runtime.getRuntime();
			stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stat.add( "Stat_Counter_ExpandAll", Record.expandAllCount );
		}
		else {
			if( DEBUG.SampleStatON ) {
				System.out.println( "[Beta] " + beta );
				System.out.println( "[Beta] JoinTime " + joinTime );
				System.out.println( "[Beta] TotalExp " + totalExp );
			}
		}
	}
}
