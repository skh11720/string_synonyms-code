package snu.kdd.substring_syn.data;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

public class Query {
	
	public final DataInfo dataInfo;
	public final Ruleset ruleSet;
	public final Dataset indexedSet;
	public final Dataset searchedSet;
	public final String outputPath;
	public final boolean selfJoin;

	public static Query parseQuery( CommandLine cmd ) throws IOException {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String searchedPath = cmd.getOptionValue( "searchedPath" );
		final String indexedPath = cmd.getOptionValue( "indexedPath" );
		final String outputPath = cmd.getOptionValue( "outputPath" );
		return new Query( rulePath, searchedPath, indexedPath, outputPath );
	}
	
	public Query( String rulePath, String searchedPath, String indexedPath, String outputPath ) throws IOException {
		this.dataInfo = new DataInfo(searchedPath, indexedPath, rulePath);
		this.outputPath = outputPath;
		TokenIndex tokenIndex = new TokenIndex();

		if ( searchedPath == null ) searchedPath = indexedPath;
		if( indexedPath.equals( searchedPath ) ) selfJoin = true;
		else selfJoin = false;
		indexedSet = new Dataset( indexedPath, tokenIndex );
		if( selfJoin ) searchedSet = indexedSet;
		else searchedSet = new Dataset( searchedPath, tokenIndex );
		ruleSet = new Ruleset( rulePath, searchedSet, tokenIndex );
		Record.tokenIndex = tokenIndex;
	}

	public void reindexByOrder( TokenOrder order ) {
		reindexRecords(order);
		reindexRules(order);
		updateTokenIndex(order);
	}
	
	private void reindexRecords( TokenOrder order ) {
		for ( Record rec : indexedSet ) rec.reindex(order);
		if ( !selfJoin ) {
			for ( Record rec : searchedSet ) rec.reindex(order);
		}
	}
	
	private void reindexRules( TokenOrder order ) {
		for ( Rule rule : ruleSet.ruleList ) rule.reindex(order);
	}
	
	private void updateTokenIndex( TokenOrder order ) {
		TokenIndex tokenIndex = order.getTokenIndex();
		Record.tokenIndex = tokenIndex;
		tokenIndex.writeToFile();
	}
	
	public String getRulePath() {
		return ruleSet.path;
	}
	
	public String getIndexedPath() {
		return indexedSet.path;
	}
	
	public String getSearchedPath() {
		return searchedSet.path;
	}
}
