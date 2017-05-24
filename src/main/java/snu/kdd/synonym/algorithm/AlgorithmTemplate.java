package snu.kdd.synonym.algorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
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
import snu.kdd.synonym.tools.Util;
import tools.DEBUG;
import tools.IntegerPair;
import tools.Rule;
import tools.Rule_ACAutomata;

public abstract class AlgorithmTemplate {
	public String outputfile;

	// Rule
	protected List<Rule> rulelist;

	// Stat container
	public StatContainer stat = null;
	protected Runtime runtime = Runtime.getRuntime();

	// String to integer mapping
	protected Object2IntOpenHashMap<String> str2int;

	// String list
	protected List<String> strlist;

	// Table S
	public List<Record> tableSearched;

	// Table T
	public List<Record> tableIndexed;

	private boolean selfJoin = false;
	protected boolean oneSideJoin = false;

	protected AlgorithmTemplate( AlgorithmTemplate o ) {
		this.str2int = o.str2int;
		this.strlist = o.strlist;
		this.tableSearched = o.tableSearched;
		this.tableIndexed = o.tableIndexed;
		this.rulelist = o.rulelist;
		this.outputfile = o.outputfile;
	}

	// Initialize rules and tables
	protected AlgorithmTemplate( String rulefile, String Rfile, String Sfile, String outputPath, DataInfo info,
			boolean oneSideJoin, StatContainer stat ) throws IOException {
		this.stat = stat;
		this.oneSideJoin = oneSideJoin;

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
			final String[] pstr = line.split( "(, | |\t)+" );
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
		tableSearched = readRecords( Rfile, size );
		tableIndexed = readRecords( Sfile, size );

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
		return tableSearched;
	}

	public List<Record> getTableS() {
		return tableIndexed;
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
		StopWatch preprocessTime = null;

		if( DEBUG.AlgorithmON ) {
			preprocessTime = StopWatch.getWatchStarted( "Result_2_1_Preprocess rule time" );
		}

		final Rule_ACAutomata automata = new Rule_ACAutomata( getRulelist() );

		long applicableRules = 0;

		// Preprocess each records in R
		for( final Record rec : tableSearched ) {
			rec.preprocessRules( automata, computeAutomataPerRecord );

			if( DEBUG.AlgorithmON ) {
				applicableRules += rec.getNumApplicableRules();
			}
		}

		if( DEBUG.AlgorithmON ) {
			preprocessTime.stopQuietAndAdd( stat );
			stat.add( "Stat_Applicable Rule TableSearched", applicableRules );
			stat.add( "Stat_Avg applicable rules", Double.toString( (double) applicableRules / tableSearched.size() ) );

			preprocessTime.resetAndStart( "Result_2_2_Preprocess length time" );
		}

		for( final Record rec : tableSearched ) {
			rec.preprocessLengths();
		}

		if( DEBUG.AlgorithmON ) {
			preprocessTime.stopQuietAndAdd( stat );

			preprocessTime.resetAndStart( "Result_2_3_Preprocess est record time" );
		}

		long maxTSize = 0;
		for( final Record rec : tableSearched ) {
			rec.preprocessEstimatedRecords();

			if( DEBUG.AlgorithmON ) {
				long est = rec.getEstimatedEquiv();

				if( maxTSize < est ) {
					maxTSize = est;
				}
			}
		}

		if( DEBUG.AlgorithmON ) {
			stat.add( "Stat_maximum Size of Table T", maxTSize );

			preprocessTime.stopQuietAndAdd( stat );
		}

		if( !compact ) {
			if( DEBUG.AlgorithmON ) {
				preprocessTime.resetAndStart( "Result_2_4_Preprocess token time" );
			}
			for( final Record rec : tableSearched ) {
				rec.preprocessAvailableTokens( maxIndex );
			}

			if( DEBUG.AlgorithmON ) {
				preprocessTime.stopQuietAndAdd( stat );
			}
		}

		if( DEBUG.AlgorithmON ) {
			preprocessTime.resetAndStart( "Result_2_5_Preprocess early pruning time" );
		}

		for( final Record rec : tableSearched ) {
			rec.preprocessSearchRanges();
			rec.preprocessSuffixApplicableRules();
		}

		if( DEBUG.AlgorithmON ) {
			preprocessTime.stopQuietAndAdd( stat );

			// Preprocess each records in S
			preprocessTime.resetAndStart( "Result_2_6_Preprocess records in S time" );
		}

		long maxSSize = 0;
		applicableRules = 0;
		for( final Record rec : tableIndexed ) {
			rec.preprocessRules( automata, computeAutomataPerRecord );
			rec.preprocessLengths();
			// rec.preprocessLastToken();
			rec.preprocessEstimatedRecords();

			if( !compact ) {
				rec.preprocessAvailableTokens( maxIndex );
			}
			rec.preprocessSearchRanges();
			rec.preprocessSuffixApplicableRules();

			if( DEBUG.AlgorithmON ) {
				applicableRules += rec.getNumApplicableRules();

				long est = rec.getEstimatedEquiv();
				if( maxSSize < est ) {
					maxSSize = est;
				}
			}
		}
		if( DEBUG.AlgorithmON ) {
			stat.add( "Stat_maximum Size of Table S", maxSSize );
			stat.add( "Stat_Applicable Rule TableIndexed", applicableRules );

			preprocessTime.stopQuietAndAdd( stat );
		}
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

	public abstract void run( String[] args );

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
		this.tableSearched = tableT;
	}

	public void setTableS( List<Record> tableS ) {
		this.tableIndexed = tableS;
	}

	public void writeResult( Collection<IntegerPair> rslt ) {

		stat.addPrimary( "Final Result Size", rslt.size() );

		try {
			if( DEBUG.AlgorithmON ) {
				Util.printLog( "Writing results " + rslt.size() );
			}

			final BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			bw.write( rslt.size() + "\n" );
			for( final IntegerPair ip : rslt ) {
				final Record r = tableSearched.get( ip.i1 );
				final Record s = tableIndexed.get( ip.i2 );

				if( selfJoin && r.equals( s ) ) {
					continue;
				}

				bw.write( r.toString( strlist ) + "\t==\t" + s.toString( strlist ) + "\n" );
			}
			bw.close();
		}
		catch( final Exception e ) {
			e.printStackTrace();
			Util.printLog( "Error: " + e.getMessage() );
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

	public void printGCStats() {
		long totalGarbageCollections = 0;
		long garbageCollectionTime = 0;

		for( GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans() ) {

			long count = gc.getCollectionCount();

			if( count >= 0 ) {
				totalGarbageCollections += count;
			}

			long time = gc.getCollectionTime();

			if( time >= 0 ) {
				garbageCollectionTime += time;
			}
		}

		Util.printLog( "Total Garbage Collections: " + totalGarbageCollections );
		Util.printLog( "Total Garbage Collection Time (ms): " + garbageCollectionTime );

		stat.add( "Stat_Garbage_Collections", totalGarbageCollections );
		stat.add( "Stat_Garbage_Collections_Time", garbageCollectionTime );
	}

	public long getGCCount() {
		long totalGarbageCollections = 0;
		for( GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans() ) {

			long count = gc.getCollectionCount();

			if( count >= 0 ) {
				totalGarbageCollections += count;
			}
		}
		return totalGarbageCollections;
	}

}
