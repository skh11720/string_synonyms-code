package snu.kdd.synonym.synonymRev.data;

import java.io.IOException;

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
		}
	}
}
