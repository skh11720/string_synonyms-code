package snu.kdd.synonym.synonymRev.data;

import java.io.IOException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.tools.DEBUG;

public class Query {
	public String ruleFile;
	public String indexedFile;
	public String searchedFile;
	public String outputFile;

	public Ruleset ruleSet;
	public Dataset indexedSet;
	public Dataset searchedSet;
	
	// Added for HybridJoin. HJ Koo
	public Dataset lowHighSet;
	public Dataset highHighSet;
	public Dataset targetIndexedSet;
	
	public TokenIndex tokenIndex;

	public final boolean oneSideJoin;
	public final boolean selfJoin;

	public Query( String ruleFile, String indexedFile, String searchedFile, boolean oneSideJoin, String outputFile ) throws IOException {
		this.ruleFile = ruleFile;
		this.indexedFile = indexedFile;
		this.searchedFile = searchedFile;
		this.outputFile = outputFile;

		if ( searchedFile == null ) searchedFile = indexedFile;
		if( indexedFile.equals( searchedFile ) ) selfJoin = true;
		else selfJoin = false;
		
		this.oneSideJoin = oneSideJoin;

		tokenIndex = new TokenIndex();
		Record.tokenIndex = tokenIndex;

		ruleSet = new Ruleset( ruleFile, tokenIndex );

		indexedSet = new Dataset( indexedFile, tokenIndex );
		if( selfJoin ) searchedSet = indexedSet;
		else searchedSet = new Dataset( searchedFile, tokenIndex );

		// Added for HybridJoin
		targetIndexedSet = indexedSet;
		
		if( DEBUG.PrintQueryON ) {
			System.out.println( "[Query]     ruleFile: " + ruleFile + " (" + ruleSet.size() + ")" );
			System.out.println( "[Query]  indexedFile: " + indexedFile + " (" + indexedSet.size() + ")" );
			System.out.println( "[Query] searchedFile: " + searchedFile + " (" + searchedSet.size() + ")" );
			System.out.println( "[Query]  oneSideJoin: " + oneSideJoin );
			System.out.println( "[Query]     selfJoin: " + selfJoin );
		}
	}

	public Query( Ruleset ruleSet, Dataset indexedSet, Dataset searchedSet, TokenIndex tokenIndex, boolean oneSideJoin, boolean selfJoin ) {
		this.ruleSet = ruleSet;
		this.indexedSet = indexedSet;
		this.searchedSet = searchedSet;
		this.indexedFile = indexedSet.name;
		this.searchedFile = searchedSet.name;
		
		//Added for HybridJoin
		this.lowHighSet = null;
		this.highHighSet = null;
		this.targetIndexedSet = indexedSet;
		
		this.tokenIndex = tokenIndex;
		this.oneSideJoin = oneSideJoin;
		this.selfJoin = selfJoin;
		
		if( DEBUG.PrintQueryON ) {
			System.out.println( "[Query]     ruleFile: rules (" + ruleSet.size() + ")" );
			System.out.println( "[Query]  indexedFile: sampled one (" + indexedSet.size() + ")" );
			System.out.println( "[Query] searchedFile: sampled two (" + searchedSet.size() + ")" );
			System.out.println( "[Query]  oneSideJoin: " + oneSideJoin );
			System.out.println( "[Query]     selfJoin: " + selfJoin );
		}
	}
	
	// Added for HybridJoin
	public void divideIndexedSet(int threshold) {
		ObjectArrayList<Record> lowhighList = new ObjectArrayList<Record>(); 
		ObjectArrayList<Record> highhighList = new ObjectArrayList<Record>();
		for ( Record s : this.indexedSet.get() ){
			if ( s.getEstNumTransformed() <= threshold ){
				lowhighList.add( s );
			}
			else {
				highhighList.add( s );
			}
		}
		this.lowHighSet = new Dataset( lowhighList );
		this.highHighSet = new Dataset( highhighList );
		
		if ( this.indexedSet.nRecord != this.lowHighSet.nRecord + this.highHighSet.nRecord ) {
			System.out.println( "Length MissMatch! in Query::divideIndexedSet - HJ Koo" );
		}
	}
	
	// Added for HybridJoin
	public void setTargetIndexSet( int mode ) {
		if ( mode == 0 ) {
			// Original Set
			this.targetIndexedSet = this.indexedSet;	
		}
		else if ( mode == 1 ) {
			// LowHigh Set
			this.targetIndexedSet = this.lowHighSet;
		}
		else if ( mode == 2 ) {
			// HighHigh Set
			this.targetIndexedSet = this.highHighSet;
		}
	}
}
