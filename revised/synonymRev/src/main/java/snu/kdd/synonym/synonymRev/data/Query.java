package snu.kdd.synonym.synonymRev.data;

public class Query {
	public String ruleFile;
	public String indexedFile;
	public String searchedFile;
	public String outputFile;

	public boolean oneSideJoin;
	public boolean selfJoin;

	public Query( String ruleFile, String indexedFile, String searchedFile, boolean oneSideJoin, String outputFile ) {
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
	}
}
