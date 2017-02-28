package snu.kdd.synonym.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.data.DataInfo;

public class StatContainer {
	private ObjectArrayList<String> primaryNameList = new ObjectArrayList<>();
	private ObjectArrayList<String> primaryValueList = new ObjectArrayList<>();
	private ObjectArrayList<String> nameList = new ObjectArrayList<>();
	private ObjectArrayList<String> valueList = new ObjectArrayList<>();

	public void add( String name, String value ) {
		nameList.add( name );
		valueList.add( value );
	}

	public void add( String name, long value ) {
		nameList.add( name );
		valueList.add( Long.toString( value ) );
	}

	public void add( StopWatch time ) {
		add( time.getName(), time.getTotalTime() );
	}

	public void add( DataInfo dataInfo ) {
		// TODO
	}

	public void add( CommandLine cmd ) {
		Iterator<Option> itr = cmd.iterator();

		while( itr.hasNext() ) {
			Option opt = itr.next();

			String name = "cmd_" + opt.getOpt();

			String valueName = opt.getValue();
			if( valueName == null ) {
				valueName = new String( "null" );
			}

			add( name, valueName );
		}
	}

	public void addPrimary( String name, String value ) {
		primaryNameList.add( name );
		primaryValueList.add( value );
	}

	public void addPrimary( String name, long value ) {
		primaryNameList.add( name );
		primaryValueList.add( Long.toString( value ) );
	}

	public void addPrimary( StopWatch sw ) {
		primaryNameList.add( sw.getName() );
		primaryValueList.add( Long.toString( sw.getTotalTime() ) );
	}

	public String toJson() {
		StringBuilder bld = new StringBuilder();
		for( int i = 0; i < primaryNameList.size(); i++ ) {
			if( i != 0 ) {
				bld.append( "," );
			}
			bld.append( "\"" + primaryNameList.get( i ) + "\":" );
			bld.append( "\"" + primaryValueList.get( i ) + "\"" );
		}

		for( int i = 0; i < nameList.size(); i++ ) {
			if( i != 0 || ( primaryNameList.size() != 0 ) ) {
				bld.append( "," );
			}
			bld.append( "\"" + nameList.get( i ) + "\":" );
			bld.append( "\"" + valueList.get( i ) + "\"" );
		}
		return bld.toString();
	}

	public void printPrimaryResult() {
		int maxKeyLength = 0;
		for( int i = 0; i < primaryNameList.size(); i++ ) {
			int length = primaryNameList.get( i ).length();
			if( maxKeyLength < length ) {
				maxKeyLength = length;
			}
		}

		for( int i = 0; i < primaryNameList.size(); i++ ) {
			int length = primaryNameList.get( i ).length();
			System.out.print( primaryNameList.get( i ) );

			for( int f = length; f < maxKeyLength; f++ ) {
				System.out.print( ' ' );
			}

			System.out.print( "  " );
			System.out.println( primaryValueList.get( i ) );
		}
	}

	public void printResult() {
		int maxKeyLength = 0;
		for( int i = 0; i < nameList.size(); i++ ) {
			int length = nameList.get( i ).length();
			if( maxKeyLength < length ) {
				maxKeyLength = length;
			}
		}

		for( int i = 0; i < nameList.size(); i++ ) {
			int length = nameList.get( i ).length();
			System.out.print( nameList.get( i ) );

			for( int f = length; f < maxKeyLength; f++ ) {
				System.out.print( ' ' );
			}

			System.out.print( "  " );
			System.out.println( valueList.get( i ) );
		}
	}

	public void resultWriter( String filename ) {
		final boolean exists = ( new File( filename ) ).exists();

		String prevLegend = "";
		if( exists ) {
			BufferedReader br = null;
			try {
				br = new BufferedReader( new FileReader( filename ) );
			}
			catch( FileNotFoundException e1 ) {
				e1.printStackTrace();
				return;
			}
			try {
				String temp = null;
				while( ( temp = br.readLine() ) != null ) {
					if( temp.startsWith( "#" ) ) {
						prevLegend = temp;
					}
				}
			}
			catch( Exception e ) {
				prevLegend = "";
			}
			try {
				br.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		PrintStream dataFile = null;
		try {
			dataFile = new PrintStream( new FileOutputStream( filename, true ) );
		}
		catch( FileNotFoundException e1 ) {
			e1.printStackTrace();
			return;
		}

		final int[] primarykeyblank = new int[ primaryNameList.size() ];
		final int[] primaryvalueblank = new int[ primaryValueList.size() ];
		for( int i = 0; i < primaryNameList.size(); i++ ) {
			primarykeyblank[ i ] = primaryValueList.get( i ).length() - primaryNameList.get( i ).length() - 2;
			primaryvalueblank[ i ] = primaryNameList.get( i ).length() + 2 - primaryValueList.get( i ).length();
		}

		final int[] keyblank = new int[ nameList.size() ];
		final int[] valueblank = new int[ valueList.size() ];
		for( int i = 0; i < nameList.size(); i++ ) {
			keyblank[ i ] = valueList.get( i ).length() - nameList.get( i ).length() - 2;
			valueblank[ i ] = nameList.get( i ).length() + 2 - valueList.get( i ).length();
		}

		String legend = getLegend( primarykeyblank, keyblank );

		if( !legend.equals( prevLegend ) ) {
			// print legend
			dataFile.println( legend );

			System.out.println( "prevLegend : " + prevLegend );
			System.out.println( "    legend : " + legend );
		}

		try {
			if( primaryValueList.size() != 0 ) {
				dataFile.print( primaryValueList.get( 0 ) );
				appendBlank( dataFile, primaryvalueblank[ 0 ] + 1 );

				for( int i = 1; i < primaryValueList.size(); i++ ) {
					dataFile.print( "  " + primaryValueList.get( i ) );
					appendBlank( dataFile, primaryvalueblank[ i ] );
				}
			}
			if( valueList.size() != 0 ) {
				for( int i = 0; i < valueList.size(); i++ ) {
					dataFile.print( "  " + valueList.get( i ) );
					appendBlank( dataFile, valueblank[ i ] );
				}
				dataFile.println( "" );
			}
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
		finally {
			dataFile.close();
		}
	}

	public String getLegend( int[] primarykeyblank, int[] keyblank ) {
		StringBuilder legendBuilder = new StringBuilder();
		for( int i = 0; i < primaryNameList.size(); i++ ) {
			if( i == 0 ) {
				legendBuilder.append( "#\"" + primaryNameList.get( i ) + "\"" );
			}
			else {
				legendBuilder.append( "  \"" + primaryNameList.get( i ) + "\"" );
			}

			appendBlank( legendBuilder, primarykeyblank[ i ] );
		}

		for( int i = 0; i < nameList.size(); i++ ) {

			legendBuilder.append( "  \"" + nameList.get( i ) + "\"" );

			appendBlank( legendBuilder, keyblank[ i ] );
		}

		return legendBuilder.toString();
	}

	public static void appendBlank( PrintStream dataFile, int blankCount ) {
		if( blankCount < 0 ) {
			return;
		}
		for( int i = 0; i < blankCount; i++ ) {
			dataFile.print( ' ' );
		}
	}

	public static void appendBlank( StringBuilder builder, int blankCount ) {
		if( blankCount < 0 ) {
			return;
		}
		for( int i = 0; i < blankCount; i++ ) {
			builder.append( ' ' );
		}
	}
}