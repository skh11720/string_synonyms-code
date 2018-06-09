package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import vldb17.set.PkduckSetDP;
import vldb17.set.PkduckSetDPWithRC;

public class JoinPQFilterDPSet2 extends JoinPQFilterDPSet {
	
	private long isInSigUTime = 0;
	private long nRunDP = 0;
	private boolean useRuleComp;

	public JoinPQFilterDPSet2( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected ParamPQFilterDPSet getParams( Query query, String[] args ) throws IOException, ParseException {
		ParamPQFilterDPSet params = super.getParams( query, args );
		useRuleComp = params.ruleComp;
		return params;
	}

	@Override
	public Set<IntegerPair> join(StatContainer stat, Query query, boolean addStat) {
		ObjectOpenHashSet<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		if ( !query.oneSideJoin ) throw new RuntimeException("UNIMPLEMENTED CASE");
		
		// S -> S' ~ T
		for ( Record recS : query.searchedSet.recordList ) {
			joinOneRecord( recS, rslt, idxT, idxCountT );
		}

		if ( !query.selfJoin ) {
			// T -> T' ~ S
			for ( Record recT : query.indexedSet.recordList ) {
				joinOneRecord( recT, rslt, idxS, idxCountS );
			}
		}
		
		if ( addStat ) {
			stat.add( "Result_3_3_CandTokenTime", candTokenTime );
			stat.add( "Result_3_4_IsInSigUTime", isInSigUTime/1e6 );
			stat.add( "Result_3_5_CountingTime", CountingTime );
			stat.add( "Result_3_6_ValidateTime", validateTime );
			stat.add( "Result_3_7_nScanList", nScanList );
			stat.add( "Result_3_8_nRunDP", nRunDP );
		}
		return rslt;
	}

	@Override
	protected Object2IntOpenHashMap<Record> getCount( Record rec, WYK_HashMap<Integer, List<Record>> idx, IntOpenHashSet candidateTokens ) {
		Object2IntOpenHashMap<Record> count = new Object2IntOpenHashMap<Record>();
		PkduckSetDP pkduckSetDP;
		if ( useRuleComp ) pkduckSetDP = new PkduckSetDPWithRC( rec, globalOrder );
		else pkduckSetDP = new PkduckSetDP( rec, globalOrder );
		count.defaultReturnValue(0);
		for ( int token : candidateTokens ) {
			if ( !idx.containsKey( token ) ) continue;
			long startDPTime = System.nanoTime();
			boolean isInSigU = pkduckSetDP.isInSigU( token );
			++nRunDP;
			isInSigUTime += System.nanoTime() - startDPTime;
			if ( isInSigU ) {
				for ( Record recOther : idx.get( token ) ) {
					count.addTo( recOther, 1 );
				}
				++nScanList;
			}
		}
		return count;
	}

	@Override
	public String getName() {
		return "JoinPQFilterDPSet2";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 */
		return "1.00";
	}
}
