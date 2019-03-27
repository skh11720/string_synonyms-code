package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaPQVPQIndex extends JoinDeltaVarBKIndex {
	/**
	 * Use both SimpleIndex and VarBKIndex
	 */
	
	JoinDeltaSimpleIndex simpleIdx;

	public JoinDeltaPQVPQIndex( Query query, int indexK, int qSize, int deltaMax, String dist, double sampleB ) {
		super(query, indexK, qSize, deltaMax, dist, sampleB);
		simpleIdx = new JoinDeltaSimpleIndex(qSize, deltaMax, query);
	}
	
	@Override
	public void joinOneRecord( Record recS, ResultSet rslt, Validator checker ) {
	    long ts = System.nanoTime();
	    
	    // enumerate candidate positional qgrams
		List<List<QGram>> availableQGrams = simpleIdx.getCandidatePQGrams(recS);
		List<List<Set<QGram>>> availableVQGrams = getAvailableQGrams(recS);
		for (List<QGram> list : availableQGrams) {
			candQGramCount += list.size();
		}
		for (List<Set<QGram>> cand_pos : availableVQGrams) {
			for (Set<QGram> qgramSet : cand_pos) {
				candQGramCount += qgramSet.size();
			}
		}
		long afterCandQgramTime = System.nanoTime();
		
		// count candidates
		Object2IntOpenHashMap<Record> candidatesCountPQ = simpleIdx.getCandidatesCount(recS, availableQGrams);
		Set<Record> candidates = simpleIdx.getCandidates(recS, candidatesCountPQ);
		Object2IntOpenHashMap<Record> candidatesCountVPQ = getCandidatesCount(recS, availableVQGrams, candidates);
		candidates = getCandidates(recS, candidatesCountVPQ);
		long afterFilterTime = System.nanoTime();

		verify(recS, candidates, checker, rslt);
		long afterVerifyTime = System.nanoTime();

		candQGramCountTime += afterCandQgramTime - ts;
		filterTime += afterFilterTime - afterCandQgramTime;
		verifyTime += afterVerifyTime - afterFilterTime;
	}

	@Override
	protected void postprocessAfterJoin( StatContainer stat ) {
		stat.add(Stat.INDEX_SIZE, simpleIdx.sizeIdxByPQGram()+indexSize() );
		stat.add(Stat.LEN_INDEX_SIZE, simpleIdx.sizeIdxByLen()+shortList.size() );
		stat.add(Stat.CAND_PQGRAM_COUNT, candQGramCount );
		stat.add(Stat.CAND_BY_PQGRAM, nCandByPQF );
		stat.add(Stat.CAND_BY_LEN, nCandByLen );
		stat.add(Stat.CAND_PQGRAM_TIME, candQGramCountTime/1e6 );
		stat.add(Stat.FILTER_TIME, filterTime/1e6 );
		stat.add(Stat.VERIFY_TIME, verifyTime/1e6 );
		stat.add( "posDistribution", posCounter.toString() );
	}
}
