package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class AlgorithmBidirectionWrapper extends AbstractAlgorithm {
	
	private AlgorithmInterface alg;
	
	public AlgorithmBidirectionWrapper( AlgorithmInterface alg ) {
		this.alg = alg;
		alg.setWriteResult(true);
	}
	
	@Override
	public void initialize() {
		// NOT USED
	}
	
	@Override
	public void run( Query query ) {
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
		ResultSet rslt1 = alg.getResult();
		StatContainer stat1 = alg.getStat();
		
		// indexedSet -> searchedSet
		alg.run(queryRev);
		ResultSet rslt2 = alg.getResult();
		StatContainer stat2 = alg.getStat();
		
//		 DEBUG: check unidirectional results
		checkUnidirResults(stat1, stat2);

		// merge results
		rslt = mergeResults(rslt1, rslt2);
		stat = mergeStats(stat1, stat2);
		
		// output results
		this.query = query;
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
	
	private ResultSet mergeResults(ResultSet rslt1, ResultSet rslt2) {
		ResultSet rslt = new ResultSet(false);
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
	public ResultSet getResult() {
		return rslt;
	}

	@Override
	public StatContainer getStat() {
		return stat;
	}

	@Override
	public void setWriteResult(boolean flag) {
		this.writeResultOn = flag;
		alg.setWriteResult(flag);
	}

	@Override
	public String getVersion() {
		return alg.getVersion();
	}

	@Override
	public String getName() {
		return alg.getName();
	}
	
	@Override
	public String getNameWithParam() {
		return String.format("merged_%s", alg.getNameWithParam());
	}
}
