package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.DataInfo;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class AlgorithmSemiUniWrapper implements AlgorithmInterface {
	
	private AlgorithmTemplate alg;
	private Collection<IntegerPair> rslt;
	
	public AlgorithmSemiUniWrapper( AlgorithmTemplate alg ) {
		this.alg = alg;
		rslt = new ObjectOpenHashSet<>();
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		// searchedSet -> indexedSet
		alg.run( query, args );
		rslt.addAll( alg.rslt );
		StatContainer stat1 = alg.stat;
		
		// intermission
		Query queryInv = new Query( query.ruleSet, query.searchedSet, query.indexedSet, query.tokenIndex, query.oneSideJoin, query.selfJoin ); // S and T are switched
		queryInv.outputFile = query.outputFile;
		alg.rslt = null;
		alg.stat = new StatContainer();
		alg.query = queryInv;
		
		// indexedSet -> searchedSet
		alg.run( queryInv, args );
		rslt.addAll( alg.rslt );
		StatContainer stat2 = alg.stat;

		// finalize
		stat1.merge( stat2, rslt );
		alg.stat = stat1;
	}

	@Override
	public void printStat() {
		alg.printStat();
	}

	@Override
	public void writeJSON( DataInfo dataInfo, CommandLine cmd ) {
		alg.writeJSON( dataInfo, cmd );
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
	public Collection<IntegerPair> getResult() {
		return rslt;
	}

	@Override
	public void setWriteResult( boolean flag ) {
		alg.writeResult = flag;
	}
}
