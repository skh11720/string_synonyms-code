package snu.kdd.synonym.algorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mine.Record;
import snu.kdd.synonym.tools.StatContainer;
import tools.IntegerPair;
import tools.Rule;
import tools.Rule_ACAutomata;

public abstract class AlgorithmTemplate {
	// String to integer mapping
	protected Object2IntOpenHashMap<String> str2int;

	// String list
	protected List<String> strlist;

	// Table R
	protected List<Record> tableR;

	// Table S
	protected List<Record> tableS;

	// Rule
	private List<Rule> rulelist;

	// Stat container
	protected StatContainer stat = null;

	protected String outputfile;

	public abstract String getVersion();

	public abstract String getName();

	public abstract void run( String[] args, StatContainer stat );

	// Initialize rules and tables
	protected AlgorithmTemplate( String rulefile, String Rfile, String Sfile, String outputfile ) throws IOException {
		this.outputfile = outputfile + "/" + this.getName();

		str2int = new Object2IntOpenHashMap<String>();
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

	protected AlgorithmTemplate( AlgorithmTemplate o ) {
		this.str2int = o.str2int;
		this.strlist = o.strlist;
		this.tableR = o.tableR;
		this.tableS = o.tableS;
		this.setRulelist( o.getRulelist() );
	}

	/**
	 * Common preprocess:</br>
	 * 1) find applicable rules</br>
	 * 2) available lengths</br>
	 * 3) number of 1-expanded records</br>
	 * 4) search ranges</br>
	 */

	protected void preprocess( boolean compact, int maxIndex, boolean computeAutomataPerRecord ) {
		Rule_ACAutomata automata = new Rule_ACAutomata( getRulelist() );

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
		setRulelist( new ArrayList<Rule>() );
		BufferedReader br = new BufferedReader( new FileReader( Rulefile ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			getRulelist().add( new Rule( line, str2int ) );
		}
		br.close();

		// Add Self rule
		for( int token : str2int.values() )
			getRulelist().add( new Rule( token, token ) );
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
		return str2int.getInt( str );
	}

	public void printStat() {
		System.out.println( "=============[" + this.getName() + " stats" + "]=============" );
		stat.printResult();
		System.out.println( "==============" + this.getName().replaceAll( ".", "=" ) + "====================" );
	}

	public void writeResult( Collection<IntegerPair> rslt ) {
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			for( IntegerPair ip : rslt ) {
				Record r = tableR.get( ip.i1 );
				Record s = tableS.get( ip.i2 );
				if( !r.equals( s ) )
					bw.write(
							tableR.get( ip.i1 ).toString( strlist ) + "\t==\t" + tableS.get( ip.i2 ).toString( strlist ) + "\n" );
			}
			bw.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}

	public Object2IntOpenHashMap<String> getStr2int() {
		return str2int;
	}

	public void setStr2int( Object2IntOpenHashMap<String> str2int ) {
		this.str2int = str2int;
	}

	public List<String> getStrlist() {
		return strlist;
	}

	public void setStrlist( List<String> strlist ) {
		this.strlist = strlist;
	}

	public List<Record> getTableR() {
		return tableR;
	}

	public void setTableR( List<Record> tableR ) {
		this.tableR = tableR;
	}

	public List<Record> getTableS() {
		return tableS;
	}

	public void setTableS( List<Record> tableS ) {
		this.tableS = tableS;
	}

	public List<Rule> getRulelist() {
		return rulelist;
	}

	public void setRulelist( List<Rule> rulelist ) {
		this.rulelist = rulelist;
	}
}
