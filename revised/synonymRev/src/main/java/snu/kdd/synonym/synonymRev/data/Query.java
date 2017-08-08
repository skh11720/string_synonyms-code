package snu.kdd.synonym.synonymRev.data;

import java.io.IOException;

import snu.kdd.synonym.synonymRev.tools.DEBUG;

public class Query {
	public String ruleFile;
	public String indexedFile;
	public String searchedFile;
	public String outputFile;

	public Ruleset ruleSet;
	public Dataset indexedSet;
	public Dataset searchedSet;
	public TokenIndex tokenIndex;

	public boolean oneSideJoin;
	public boolean selfJoin;

	public Query( String ruleFile, String indexedFile, String searchedFile, boolean oneSideJoin, String outputFile )
			throws IOException {
		this.ruleFile = ruleFile;
		this.indexedFile = indexedFile;
		this.searchedFile = searchedFile;
		this.outputFile = outputFile;

		if( indexedFile.equals( searchedFile ) ) {
			selfJoin = true;
		}
		else {
			selfJoin = false;
		}

		this.oneSideJoin = oneSideJoin;

		tokenIndex = new TokenIndex();
		ruleSet = new Ruleset( ruleFile, tokenIndex );

		if( selfJoin ) {
			indexedSet = new Dataset( indexedFile, tokenIndex );
			searchedSet = indexedSet;
		}
		else {
			indexedSet = new Dataset( indexedFile, tokenIndex );
			if( searchedFile != null ) {
				searchedSet = new Dataset( searchedFile, tokenIndex );
			}
			else {
				this.selfJoin = true;
				searchedSet = indexedSet;
			}
		}

		if( DEBUG.PrintQueryON ) {
			System.out.println( "[Query]     ruleFile: " + ruleFile + " (" + ruleSet.size() + ")" );
			System.out.println( "[Query]  indexedFile: " + indexedFile + " (" + indexedSet.size() + ")" );
			System.out.println( "[Query] searchedFile: " + searchedFile + " (" + searchedSet.size() + ")" );
			System.out.println( "[Query]  oneSideJoin: " + oneSideJoin );
			System.out.println( "[Query]     selfJoin: " + selfJoin );
		}
	}

	public Query( Ruleset ruleSet, Dataset indexedSet, Dataset searchedSet, TokenIndex tokenIndex, boolean oneSideJoin,
			boolean selfJoin ) {
		this.ruleSet = ruleSet;
		this.indexedSet = indexedSet;
		this.searchedSet = searchedSet;
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
}
