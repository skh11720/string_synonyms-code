package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class AlgorithmBidirectionWrapper extends AbstractAlgorithm {
	
	private AlgorithmInterface alg;
	
	public AlgorithmBidirectionWrapper( AlgorithmInterface alg ) {
		this.alg = alg;
		alg.setWriteResult(false);
	}
	
	@Override
	public void initialize() {
		// NOT USED
	}
	
	@Override
	public void run( Query query ) {
		this.query = query;
		// TODO: skip pairs verified in a direction
		if ( query.selfJoin ) 
			throw new RuntimeException(this.getClass().getName()+" does not allow self join.");

		Query queryRev = null;
		try {
			queryRev = new Query( query.getRulePath(), query.getIndexedPath(), query.getSearchedPath(), query.oneSideJoin, query.outputPath );
		}
		catch ( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		// searchedSet -> indexedSet
		alg.run(query);
		Set<IntegerPair> rslt1 = alg.getResult();
		StatContainer stat1 = alg.getStat();
		
		// indexedSet -> searchedSet
		alg.run(queryRev);
		Set<IntegerPair> rslt2 = alg.getResult();
		StatContainer stat2 = alg.getStat();
		
//		 DEBUG: check unidirectional results
		checkUnidirResults(stat1, stat2);

		// merge results
		rslt = mergeResults(rslt1, rslt2);
		stat = mergeStats(stat1, stat2);
		writeResult();
	}
	
	@Override
	protected void executeJoin() {
	}

	private void checkUnidirResults( StatContainer stat1, StatContainer stat2 ) {
		System.out.println( "=============[" + alg.getName() + " stats1" + "]=============" );
		stat1.printResult();
		System.out.println("==============" + new String( new char[ alg.getName().length() ] ).replace( "\0", "=" ) + "====================" );

		System.out.println( "=============[" + alg.getName() + " stats2" + "]=============" );
		stat2.printResult();
		System.out.println("==============" + new String( new char[ alg.getName().length() ] ).replace( "\0", "=" ) + "====================" );
	}
	
	private Set<IntegerPair> mergeResults(Set<IntegerPair> rslt1, Set<IntegerPair> rslt2) {
		Set<IntegerPair> rslt = new ObjectOpenHashSet<>();
		rslt.addAll(rslt1);
		for ( IntegerPair ipair : rslt2 ) {
			rslt.add(new IntegerPair(ipair.i2, ipair.i1));
		}
		return rslt;
	}
	
	private StatContainer mergeStats(StatContainer stat1, StatContainer stat2) {
		StatContainer stat = StatContainer.merge(stat1, stat2);
		stat.setPrimaryValue("Final_Result_Size", Integer.toString(rslt.size()));
		return stat;
	}

	@Override
	public void writeJSON() {
		((AbstractAlgorithm)alg).stat = this.stat; // TODO: is there a better way with no casting..?
		alg.writeJSON();
	}

	@Override
	public Set<IntegerPair> getResult() {
		return rslt;
	}

	@Override
	public StatContainer getStat() {
		return stat;
	}

	@Override
	public void setWriteResult(boolean flag) {
		alg.setWriteResult(flag);
	}

	@Override
	public String getName() {
		return alg.getName();
	}
	
	@Override
	public String getVersion() {
		return alg.getVersion();
	}

	@Override
	public String getOutputName() {
		return getName()+"_merged_"+query.dataInfo.dataOneFileName.split("\\.")[0]+"_"+query.dataInfo.dataTwoFileName.split("\\.")[0];
	}
}
