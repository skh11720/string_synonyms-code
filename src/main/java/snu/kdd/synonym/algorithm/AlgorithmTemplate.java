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
import snu.kdd.synonym.tools.StopWatch;
import tools.IntegerPair;
import tools.Rule;
import tools.Rule_ACAutomata;

public abstract class AlgorithmTemplate {
	protected String outputfile;

	// Rule
	protected List<Rule> rulelist;

	// Stat container
	protected StatContainer stat = null;

	// String to integer mapping
	protected Object2IntOpenHashMap<String> str2int;

	// String list
	protected List<String> strlist;

	// Table R
	protected List<Record> tableT;

	// Table S
	protected List<Record> tableS;

	private boolean selfJoin = false;

	protected AlgorithmTemplate( AlgorithmTemplate o ) {
		System.out.println( "Initialize with o " + o.getName() );

		this.str2int = o.str2int;
		this.strlist = o.strlist;
		this.tableT = o.tableT;
		this.tableS = o.tableS;
		this.rulelist = o.rulelist;
		this.outputfile = o.outputfile;
	}

	// Initialize rules and tables
	protected AlgorithmTemplate( String rulefile, String Rfile, String Sfile, String outputPath ) throws IOException {

		str2int = new Object2IntOpenHashMap<>();
		str2int.defaultReturnValue( -1 );
		strlist = new ArrayList<>();

		// Add empty string first
		str2int.put( "", 0 );
		strlist.add( "" );

		BufferedReader br = new BufferedReader( new FileReader( rulefile ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			final String[] pstr = line.split( "(,| |\t)+" );
			for( final String str : pstr ) {
				getID( str );
			}
		}
		br.close();

		br = new BufferedReader( new FileReader( Rfile ) );
		while( ( line = br.readLine() ) != null ) {
			final String[] pstr = line.split( "( |\t)+" );
			for( final String str : pstr ) {
				getID( str );
			}
		}
		br.close();

		br = new BufferedReader( new FileReader( Sfile ) );
		while( ( line = br.readLine() ) != null ) {
			final String[] pstr = line.split( "( |\t)+" );
			for( final String str : pstr ) {
				getID( str );
			}
		}
		br.close();

		// Read records
		final int size = -1;
		Record.setStrList( strlist );
		tableT = readRecords( Rfile, size );
		tableS = readRecords( Sfile, size );

		readRules( rulefile );

		this.outputfile = outputPath + "/" + this.getName();
	}

	private int getID( String str ) {
		// Get id of str, if a new str is given add it to str2int and strlist
		int id = str2int.getInt( str );
		if( id == -1 ) {
			int newIndex = strlist.size();
			str2int.put( str, newIndex );
			strlist.add( str );

			return newIndex;
		}

		return id;
	}

	public abstract String getName();

	public List<Rule> getRulelist() {
		return rulelist;
	}

	public Object2IntOpenHashMap<String> getStr2int() {
		return str2int;
	}

	public List<String> getStrlist() {
		return strlist;
	}

	public List<Record> getTableT() {
		return tableT;
	}

	public List<Record> getTableS() {
		return tableS;
	}

	public abstract String getVersion();

	/**
	 * Common preprocess:</br>
	 * 1) find applicable rules</br>
	 * 2) available lengths</br>
	 * 3) number of 1-expanded records</br>
	 * 4) search ranges</br>
	 */

	protected void preprocess( boolean compact, int maxIndex, boolean computeAutomataPerRecord ) {
		// builds an automata of the set of rules
		StopWatch preprocessTime = StopWatch.getWatchStarted( "Preprocess rule time" );
		final Rule_ACAutomata automata = new Rule_ACAutomata( getRulelist() );

		long applicableRules = 0;
		// Preprocess each records in R
		for( final Record rec : tableT ) {
			rec.preprocessRules( automata, computeAutomataPerRecord );
			applicableRules += rec.getNumApplicableRules();
		}
		preprocessTime.stopQuietAndAdd( stat );

		System.out.println( "Avg applicable rules : " + applicableRules + "/" + tableT.size() );
		stat.add( "Avg applicable rules", applicableRules + "/" + tableT.size() );

		preprocessTime.resetAndStart( "Preprocess length time" );

		System.out.println( "Preprocessing with modified length" );
		for( final Record rec : tableT ) {
			rec.preprocessLengths();
		}

		preprocessTime.stopQuietAndAdd( stat );

		preprocessTime.resetAndStart( "Preprocess est record time" );
		for( final Record rec : tableT ) {
			rec.preprocessEstimatedRecords();
		}
		preprocessTime.stopQuietAndAdd( stat );

		if( !compact ) {
			preprocessTime.resetAndStart( "Preprocess token time" );
			for( final Record rec : tableT ) {
				rec.preprocessAvailableTokens( maxIndex );
			}
			preprocessTime.stopQuietAndAdd( stat );
		}

		preprocessTime.resetAndStart( "Preprocess early pruning time" );
		for( final Record rec : tableT ) {
			rec.preprocessSearchRanges();
			rec.preprocessSuffixApplicableRules();
		}
		preprocessTime.stopQuietAndAdd( stat );

		// Preprocess each records in S
		preprocessTime.resetAndStart( "Preprocess records in S time" );
		for( final Record rec : tableS ) {
			rec.preprocessRules( automata, computeAutomataPerRecord );
			rec.preprocessLengths();
			rec.preprocessEstimatedRecords();
			if( !compact ) {
				rec.preprocessAvailableTokens( maxIndex );
			}
			rec.preprocessSearchRanges();
			rec.preprocessSuffixApplicableRules();
		}
		preprocessTime.stopQuietAndAdd( stat );
	}

	public void printStat() {
		System.out.println( "=============[" + this.getName() + " stats" + "]=============" );
		stat.printResult();
		System.out.println( "==============" + this.getName().replaceAll( ".", "=" ) + "====================" );
	}

	protected List<Record> readRecords( String DBfile, int num ) throws IOException {
		final List<Record> rslt = new ArrayList<>();
		final BufferedReader br = new BufferedReader( new FileReader( DBfile ) );
		String line;
		while( ( line = br.readLine() ) != null && num != 0 ) {
			rslt.add( new Record( rslt.size(), line, str2int ) );
			--num;
		}
		br.close();
		return rslt;
	}

	protected void readRules( String Rulefile ) throws IOException {
		setRulelist( new ArrayList<Rule>() );
		final BufferedReader br = new BufferedReader( new FileReader( Rulefile ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			getRulelist().add( new Rule( line, str2int ) );
		}
		br.close();

		// Add Self rule
		for( final int token : str2int.values() ) {
			getRulelist().add( new Rule( token, token ) );
		}
	}

	public abstract void run( String[] args, StatContainer stat );

	public void setRulelist( List<Rule> rulelist ) {
		this.rulelist = rulelist;
	}

	public void setStr2int( Object2IntOpenHashMap<String> str2int ) {
		this.str2int = str2int;
	}

	public void setStrlist( List<String> strlist ) {
		this.strlist = strlist;
	}

	public void setTableT( List<Record> tableT ) {
		this.tableT = tableT;
	}

	public void setTableS( List<Record> tableS ) {
		this.tableS = tableS;
	}

	public void writeResult( Collection<IntegerPair> rslt ) {
		stat.addPrimary( "Final Result Size", rslt.size() );
		try {
			System.out.println( "Writing results " + rslt.size() );
			final BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			for( final IntegerPair ip : rslt ) {
				final Record r = tableT.get( ip.i1 );
				final Record s = tableS.get( ip.i2 );
				if( selfJoin && r.equals( s ) ) {
					continue;
				}
				bw.write( tableT.get( ip.i1 ).toString( strlist ) + "\t==\t" + tableS.get( ip.i2 ).toString( strlist ) + "\n" );
			}
			bw.close();
		}
		catch( final Exception e ) {
			e.printStackTrace();
			System.out.println( "Error: " + e.getMessage() );
		}
	}

	public boolean isSelfJoin() {
		return selfJoin;
	}

	public void setSelfJoin( boolean selfJoin ) {
		this.selfJoin = selfJoin;
	}
}
