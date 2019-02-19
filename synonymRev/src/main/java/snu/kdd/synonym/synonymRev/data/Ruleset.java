package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Ruleset {
	final String path;
	final ObjectArrayList<Rule> ruleList;
	boolean selfRuleAdded = false;
	int selfRuleAddedIndex = -1;

	public Ruleset( String rulePath, TokenIndex tokenIndex ) throws IOException {
		this.path = rulePath;
		this.ruleList = new ObjectArrayList<>();

		BufferedReader br = new BufferedReader( new FileReader( rulePath ) );
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
