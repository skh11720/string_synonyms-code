package snu.kdd.synonym.algorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mine.Record;
import snu.kdd.synonym.data.DataInfo;
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

	// Table S
	protected List<Record> tableX;

	// Table T
	protected List<Record> tableY;

	private boolean selfJoin = false;

	protected AlgorithmTemplate( AlgorithmTemplate o ) {
		System.out.println( "Initialize with o " + o.getName() );

		this.str2int = o.str2int;
		this.strlist = o.strlist;
		this.tableX = o.tableX;
		this.tableY = o.tableY;
		this.rulelist = o.rulelist;
		this.outputfile = o.outputfile;
	}

	// Initialize rules and tables
	protected AlgorithmTemplate( String rulefile, String Rfile, String Sfile, String outputPath, DataInfo info )
			throws IOException {

		str2int = new Object2IntOpenHashMap<>();
		str2int.defaultReturnValue( -1 );
		strlist = new ArrayList<>();

		// Add empty string first
		str2int.put( "", 0 );
		strlist.add( "" );

		BufferedReader br = new BufferedReader( new FileReader( rulefile ) );
		int ruleCount = 0;
		String line;
		while( ( line = br.readLine() ) != null ) {
			ruleCount++;
			final String[] pstr = line.split( "(,| |\t)+" );
			for( final String str : pstr ) {
				getID( str );
			}
		}
		br.close();

		info.updateRuleCount( ruleCount );

		int oneCount = 0;
		br = new BufferedReader( new FileReader( Rfile ) );
		while( ( line = br.readLine() ) != null ) {
			oneCount++;
			final String[] pstr = line.split( "( |\t)+" );
			for( final String str : pstr ) {
				getID( str );
			}
		}
		br.close();

		info.updateOneCount( oneCount );

		int twoCount = 0;
		br = new BufferedReader( new FileReader( Sfile ) );
		while( ( line = br.readLine() ) != null ) {
			twoCount++;
			final String[] pstr = line.split( "( |\t)+" );
			for( final String str : pstr ) {
				getID( str );
			}
		}
		br.close();

		info.updateTwoCount( twoCount );

		// Read records
		final int size = -1;
		Record.setStrList( strlist );
		tableX = readRecords( Rfile, size );
		tableY = readRecords( Sfile, size );

		readRules( rulefile );

		this.outputfile = outputPath + "/" + this.getName();
		info.writeInfo();
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
		return tableX;
	}

	public List<Record> getTableS() {
		return tableY;
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
		StopWatch preprocessTime = StopWatch.getWatchStarted( "Result_2_1_Preprocess rule time" );
		final Rule_ACAutomata automata = new Rule_ACAutomata( getRulelist() );

		long applicableRules = 0;
		// Preprocess each records in R
		for( final Record rec : tableX ) {
			rec.preprocessRules( automata, computeAutomataPerRecord );
			applicableRules += rec.getNumApplicableRules();
		}
		preprocessTime.stopQuietAndAdd( stat );

		// System.out.println( "Avg applicable rules : " + applicableRules + "/" + tableT.size() );
		stat.add( "Stat_Avg applicable rules", applicableRules + "/" + tableX.size() );

		preprocessTime.resetAndStart( "Result_2_2_Preprocess length time" );

		// System.out.println( "Preprocessing with modified length" );
		for( final Record rec : tableX ) {
			rec.preprocessLengths();
			// DEBUG
			// rec.preprocessLastToken();
		}

		preprocessTime.stopQuietAndAdd( stat );

		preprocessTime.resetAndStart( "Result_2_3_Preprocess est record time" );
		long maxTSize = 0;
		for( final Record rec : tableX ) {
			rec.preprocessEstimatedRecords();
			long est = rec.getEstimatedEquiv();
			if( maxTSize < est ) {
				// System.out.println( "New Maximum Estimation " + rec.getID() + " " + est );
				maxTSize = est;
			}
		}
		stat.add( "Stat_maximum Size of Table T", maxTSize );

		preprocessTime.stopQuietAndAdd( stat );

		if( !compact ) {
			preprocessTime.resetAndStart( "Result_2_4_Preprocess token time" );
			for( final Record rec : tableX ) {
				rec.preprocessAvailableTokens( maxIndex );
			}
			preprocessTime.stopQuietAndAdd( stat );
		}

		preprocessTime.resetAndStart( "Result_2_5_Preprocess early pruning time" );
		for( final Record rec : tableX ) {
			rec.preprocessSearchRanges();
			rec.preprocessSuffixApplicableRules();
		}
		preprocessTime.stopQuietAndAdd( stat );

		// Preprocess each records in S
		preprocessTime.resetAndStart( "Result_2_6_Preprocess records in S time" );
		long maxSSize = 0;
		for( final Record rec : tableY ) {
			rec.preprocessRules( automata, computeAutomataPerRecord );
			rec.preprocessLengths();
			// rec.preprocessLastToken();
			rec.preprocessEstimatedRecords();
			long est = rec.getEstimatedEquiv();
			if( maxSSize < est ) {
				// System.out.println( "New Maximum Estimation " + rec.getID() + " " + est );
				maxSSize = est;
			}

			if( !compact ) {
				rec.preprocessAvailableTokens( maxIndex );
			}
			rec.preprocessSearchRanges();
			rec.preprocessSuffixApplicableRules();
		}
		stat.add( "Stat_maximum Size of Table S", maxSSize );
		preprocessTime.stopQuietAndAdd( stat );
	}

	public void printStat() {
		System.out.println( "=============[" + this.getName() + " stats" + "]=============" );
		stat.printResult();
		System.out.println(
				"==============" + new String( new char[ getName().length() ] ).replace( "\0", "=" ) + "====================" );
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
		this.tableX = tableT;
	}

	public void setTableS( List<Record> tableS ) {
		this.tableY = tableS;
	}

	public void writeResult( Collection<IntegerPair> rslt ) {

		stat.addPrimary( "Final Result Size", rslt.size() );

		try {
			System.out.println( "Writing results " + rslt.size() );
			final BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			for( final IntegerPair ip : rslt ) {
				final Record r = tableX.get( ip.i1 );
				final Record s = tableY.get( ip.i2 );
				if( selfJoin && r.equals( s ) ) {
					continue;
				}
				bw.write( tableX.get( ip.i1 ).toString( strlist ) + "\t==\t" + tableY.get( ip.i2 ).toString( strlist ) + "\n" );
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

	public void writeJSON( DataInfo dataInfo, CommandLine cmd ) {
		BufferedWriter bw_json;
		try {
			bw_json = new BufferedWriter( new FileWriter(
					"json/" + this.getName() + "_"
							+ new java.text.SimpleDateFormat( "yyyyMMdd_HHmmss_z" ).format( new java.util.Date() ) + ".txt",
					true ) );

			bw_json.write( "{" );

			bw_json.write( "\"Date\": \"" + new Date().toString() + "\"," );

			bw_json.write( "\"Algorithm\": {" );
			bw_json.write( "\"name\": \"" + getName() + "\"," );
			bw_json.write( "\"version\": \"" + getVersion() + "\"" );
			bw_json.write( "}" );

			bw_json.write( ", \"Result\":{" );
			bw_json.write( stat.toJson() );
			bw_json.write( "}" );

			bw_json.write( ", \"Dataset\": {" );
			bw_json.write( dataInfo.toJson() );
			bw_json.write( "}" );

			bw_json.write( ", \"ParametersUsed\": {" );
			bw_json.write( "\"additional\": \"" );
			bw_json.write( cmd.getOptionValue( "additional", "" ) + "\"" );
			bw_json.write( "}" );

			bw_json.write( "}\n" );
			bw_json.close();
		}
		catch( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
