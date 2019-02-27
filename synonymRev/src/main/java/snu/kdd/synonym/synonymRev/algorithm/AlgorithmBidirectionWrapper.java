package snu.kdd.synonym.synonymRev.algorithm;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class AlgorithmBidirectionWrapper implements AlgorithmInterface {
	
	private AlgorithmInterface alg;
	private Set<IntegerPair> rslt;
	private StatContainer stat;
	
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
		if ( query.selfJoin ) 
			throw new RuntimeException(this.getClass().getName()+" does not allow self join.");
		
		Query queryRev = new Query( query.ruleSet, query.searchedSet, query.indexedSet, query.tokenIndex, query.oneSideJoin, query.selfJoin, query.outputPath );

		// searchedSet -> indexedSet
		alg.run(query);
		Set<IntegerPair> rslt1 = alg.getResult();
		StatContainer stat1 = alg.getStat();
		
		// indexedSet -> searchedSet
		alg.run(queryRev);
		Set<IntegerPair> rslt2 = alg.getResult();
		StatContainer stat2 = alg.getStat();
		
		// DEBUG: check results
//		System.out.println( "=============[" + alg.getName() + " stats1" + "]=============" );
//		stat1.printResult();
//		System.out.println("==============" + new String( new char[ alg.getName().length() ] ).replace( "\0", "=" ) + "====================" );
//
//		System.out.println( "=============[" + alg.getName() + " stats2" + "]=============" );
//		stat2.printResult();
//		System.out.println("==============" + new String( new char[ alg.getName().length() ] ).replace( "\0", "=" ) + "====================" );

		// merge results
		rslt = new ObjectOpenHashSet<>();
		rslt.addAll(rslt1);
		rslt.addAll(rslt2);
		stat = StatContainer.merge(stat1, stat2);
		stat.setPrimaryValue("Final_Result_Size", Integer.toString(rslt.size()));

//		System.out.println( "=============[" + alg.getName() + " stats_merged" + "]=============" );
//		stat.printResult();
//		System.out.println("==============" + new String( new char[ alg.getName().length() ] ).replace( "\0", "=" ) + "====================" );

	}

	@Override
	public void writeJSON() {
		// TODO Auto-generated method stub
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
}
