package sigmod13;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mine.Record;
import tools.Rule;
import tools.Rule_ACAutomata;

public class SIAlgorithm {
	protected Map<String, Integer> str2int;
	protected List<String> strlist;
	protected List<SIRecord> tableT;
	protected List<SIRecord> tableS;
	protected List<Rule> rulelist;

	protected SIAlgorithm( String rulefile, String Rfile, String Sfile ) throws IOException {
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
		tableT = readRecords( Rfile, size );
		tableS = readRecords( Sfile, size );
		readRules( rulefile );
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

	protected List<SIRecord> readRecords( String DBfile, int num ) throws IOException {
		Rule_ACAutomata ruleAC = new Rule_ACAutomata( rulelist );
		List<SIRecord> rslt = new ArrayList<SIRecord>();
		BufferedReader br = new BufferedReader( new FileReader( DBfile ) );
		String line;
		while( ( line = br.readLine() ) != null && num != 0 ) {
			rslt.add( new SIRecord( rslt.size(), line, str2int, ruleAC ) );
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
