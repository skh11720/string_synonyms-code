package snu.kdd.synonym.synonymRev.data;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import snu.kdd.synonym.synonymRev.tools.DEBUG;

public class Query {
	public final Ruleset ruleSet;
	public final Dataset indexedSet;
	public final Dataset searchedSet;

	public final String outputPath;
	
	public final TokenIndex tokenIndex;

	public final boolean oneSideJoin;
	public final boolean selfJoin;

	public static Query parseQuery( CommandLine cmd ) throws IOException {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String dataOnePath = cmd.getOptionValue( "dataOnePath" );
		final String dataTwoPath = cmd.getOptionValue( "dataTwoPath" );
		final String outputPath = cmd.getOptionValue( "outputPath" );
		Boolean oneSideJoin = Boolean.parseBoolean( cmd.getOptionValue( "oneSideJoin" ) );
		return new Query( rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath );
	}

	public Query( String rulePath, String indexedPath, String searchedPath, boolean oneSideJoin, String outputPath ) throws IOException {
		this.outputPath = outputPath;
		this.oneSideJoin = oneSideJoin;
		this.tokenIndex = new TokenIndex();
		Record.tokenIndex = this.tokenIndex;

		if ( searchedPath == null ) searchedPath = indexedPath;
		if( indexedPath.equals( searchedPath ) ) selfJoin = true;
		else selfJoin = false;
		ruleSet = new Ruleset( rulePath, tokenIndex );
		indexedSet = new Dataset( indexedPath, tokenIndex );
		if( selfJoin ) searchedSet = indexedSet;
		else searchedSet = new Dataset( searchedPath, tokenIndex );

		printDebugInfo();
	}

	public Query( Ruleset ruleSet, Dataset indexedSet, Dataset searchedSet, TokenIndex tokenIndex, boolean oneSideJoin, boolean selfJoin, String outputPath ) {
		this.ruleSet = ruleSet;
		this.indexedSet = indexedSet;
		this.searchedSet = searchedSet;
		this.tokenIndex = tokenIndex;
		this.oneSideJoin = oneSideJoin;
		this.outputPath = outputPath;
		this.selfJoin = selfJoin;
		
		printDebugInfo();
	}
	
	private void printDebugInfo() {
		if( DEBUG.PrintQueryON ) {
			System.out.println( "[Query]     rulePath: rules (" + ruleSet.size() + ")" );
			System.out.println( "[Query]  indexedPath: sampled one (" + indexedSet.size() + ")" );
			System.out.println( "[Query] searchedPath: sampled two (" + searchedSet.size() + ")" );
			System.out.println( "[Query]  oneSideJoin: " + oneSideJoin );
			System.out.println( "[Query]     selfJoin: " + selfJoin );
		}
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
