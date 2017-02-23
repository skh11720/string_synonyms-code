package tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mine.Record;

public abstract class Algorithm {
	protected Map<String, Integer> str2int;
	protected List<String> strlist;
	protected List<Record> tableR;
	protected List<Record> tableS;
	protected List<Rule> rulelist;

	public abstract String getVersion();

	public abstract String getName();

	public abstract void run( String[] args );

	protected Algorithm( String rulefile, String Rfile, String Sfile ) throws IOException {
		str2int = new HashMap<String, Integer>();
		strlist = new ArrayList<String>();
		// Add empty string first
		str2int.put( "", 0 );
		strlist.add( "" );

		BufferedReader br = new BufferedReader( new FileReader( rulefile ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			String[] pstr = line.split( "(,| |\t)+" );
			for( String str : pstr )
				getID( str );
		}
		br.close();

		br = new BufferedReader( new FileReader( Rfile ) );
		while( ( line = br.readLine() ) != null ) {
			String[] pstr = line.split( "( |\t)+" );
			for( String str : pstr )
				getID( str );
		}
		br.close();

		br = new BufferedReader( new FileReader( Sfile ) );
		while( ( line = br.readLine() ) != null ) {
			String[] pstr = line.split( "( |\t)+" );
			for( String str : pstr )
				getID( str );
		}
		br.close();

		// Read records
		int size = -1;
		Record.setStrList( strlist );
		tableR = readRecords( Rfile, size );
		tableS = readRecords( Sfile, size );
		readRules( rulefile );
	}

	protected Algorithm( Algorithm o ) {
		this.str2int = o.str2int;
		this.strlist = o.strlist;
		this.tableR = o.tableR;
		this.tableS = o.tableS;
		this.rulelist = o.rulelist;
	}

	/**
	 * Common preprocess:</br>
	 * 1) find applicable rules</br>
	 * 2) available lengths</br>
	 * 3) number of 1-expanded records</br>
	 * 4) search ranges</br>
	 */
	protected void preprocess( boolean compact, int maxIndex, boolean computeAutomataPerRecord ) {
		Rule_ACAutomata automata = new Rule_ACAutomata( rulelist );

		long currentTime = System.currentTimeMillis();
		long applicableRules = 0;
		// Preprocess each records in R
		for( Record rec : tableR ) {
			rec.preprocessRules( automata, computeAutomataPerRecord );
			applicableRules += rec.getNumApplicableRules();
		}
		long time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess rules : " + time );
		System.out.println( "Avg applicable rules : " + applicableRules + "/" + tableR.size() );

		currentTime = System.currentTimeMillis();
		for( Record rec : tableR ) {
			rec.preprocessLengths();
		}
		time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess lengths: " + time );

		currentTime = System.currentTimeMillis();
		for( Record rec : tableR ) {
			rec.preprocessEstimatedRecords();
		}
		time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess est records: " + time );

		if( !compact ) {
			currentTime = System.currentTimeMillis();
			for( Record rec : tableR ) {
				rec.preprocessAvailableTokens( maxIndex );
			}
			time = System.currentTimeMillis() - currentTime;
			System.out.println( "Preprocess tokens: " + time );
		}

		currentTime = System.currentTimeMillis();
		for( Record rec : tableR ) {
			rec.preprocessSearchRanges();
			rec.preprocessSuffixApplicableRules();
		}
		time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess for early pruning: " + time );

		// Preprocess each records in S
		for( Record rec : tableS ) {
			rec.preprocessRules( automata, computeAutomataPerRecord );
			rec.preprocessLengths();
			rec.preprocessEstimatedRecords();
			if( !compact )
				rec.preprocessAvailableTokens( maxIndex );
			rec.preprocessSearchRanges();
			rec.preprocessSuffixApplicableRules();
		}
	}

	protected void readRules( String Rulefile ) throws IOException {
		rulelist = new ArrayList<Rule>();
		BufferedReader br = new BufferedReader( new FileReader( Rulefile ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			rulelist.add( new Rule( line, str2int ) );
		}
		br.close();

		// Add Self rule
		for( int token : str2int.values() )
			rulelist.add( new Rule( token, token ) );
	}

	protected List<Record> readRecords( String DBfile, int num ) throws IOException {
		List<Record> rslt = new ArrayList<Record>();
		BufferedReader br = new BufferedReader( new FileReader( DBfile ) );
		String line;
		while( ( line = br.readLine() ) != null && num != 0 ) {
			rslt.add( new Record( rslt.size(), line, str2int ) );
			--num;
		}
		br.close();
		return rslt;
	}

	private int getID( String str ) {
		if( !str2int.containsKey( str ) ) {
			str2int.put( str, strlist.size() );
			strlist.add( str );
		}
		return str2int.get( str );
	}
}
