package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Ruleset {
	String name;
	ObjectArrayList<Rule> ruleList;
	boolean selfRuleAdded = false;
	int selfRuleAddedIndex = -1;

	public Ruleset( String ruleFile, TokenIndex tokenIndex ) throws IOException {
		this.name = ruleFile;

		BufferedReader br = new BufferedReader( new FileReader( ruleFile ) );
		ruleList = new ObjectArrayList<>();

		String line;
		while( ( line = br.readLine() ) != null ) {
			ruleList.add( new Rule( line, tokenIndex ) );
		}
		br.close();
	}

	public Iterable<Rule> get() {
		return ruleList;
	}

	public int size() {
		return ruleList.size();
	}
}
