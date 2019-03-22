package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Ruleset {
	final String path;
	final ObjectArrayList<Rule> ruleList;

	public Ruleset( String rulePath, Dataset searchedSet, TokenIndex tokenIndex ) throws IOException {
		this.path = rulePath;
		this.ruleList = new ObjectArrayList<>();
		
		createSelfRules(searchedSet);
		loadRulesFromFile(tokenIndex);
	}
	
	private void createSelfRules( Dataset searchedSet ) {
		IntOpenHashSet processedTokenSet = new IntOpenHashSet();
		for ( Record recS : searchedSet.recordList ) {
			for ( int token : recS.getTokens() ) {
				if ( !processedTokenSet.contains(token) ) {
					processedTokenSet.add(token);
					ruleList.add( new Rule(token, token) );
				}
			}
		}
	}

	private void loadRulesFromFile( TokenIndex tokenIndex ) throws IOException {
		BufferedReader br = new BufferedReader( new FileReader( path ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			this.ruleList.add( new Rule( line, tokenIndex ) );
		}
		br.close();
	}

	public Iterable<Rule> get() {
		return this.ruleList;
	}

	public int size() {
		return this.ruleList.size();
	}
}
