package snu.kdd.synonym.synonymRev.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StatContainer {
	private final ObjectArrayList<String> nameList = new ObjectArrayList<>();
	private final ObjectArrayList<String> valueList = new ObjectArrayList<>();

	private final ObjectArrayList<String> primaryNameList = new ObjectArrayList<>();
	private final ObjectArrayList<String> primaryValueList = new ObjectArrayList<>();

	private static Runtime runtime = Runtime.getRuntime();

	public void add( StopWatch time ) {
		add( time.getName(), time.getTotalTime() );
	}
	
	public void add( String name, boolean value ) {
		nameList.add( name );
		valueList.add( Boolean.toString(value) );
	}

	public void add( String name, int value ) {
		nameList.add( name );
		valueList.add( Integer.toString( value ) );
	}

	public void add( String name, long value ) {
		nameList.add( name );
		valueList.add( Long.toString( value ) );
	}
	
	public void addMemory( String name ) {
		add( name, ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
	}

	public void add( String name, double value ) {
		nameList.add( name );
		valueList.add( Double.toString( value ) );
	}

	public void add( String name, String value ) {
		nameList.add( name );
		valueList.add( value );
	}

	public void addPrimary( StopWatch sw ) {
		primaryNameList.add( sw.getName() );
		primaryValueList.add( Long.toString( sw.getTotalTime() ) );
	}

	public void addPrimary( String name, long value ) {
		primaryNameList.add( name );
		primaryValueList.add( Long.toString( value ) );
	}

	public void addPrimary( String name, String value ) {
		primaryNameList.add( name );
		primaryValueList.add( value );
	}
	
	public long getLong( String key ) {
		for ( int i=0; i<nameList.size(); ++i ) {
			if ( nameList.get(i).equals(key) ) return Long.parseLong(valueList.get(i));
		}
		throw new RuntimeException("StatContainer has no such key: "+key);
	}
	
	public double getDouble( String key ) {
		for ( int i=0; i<nameList.size(); ++i ) {
			if ( nameList.get(i).equals(key) ) return Double.parseDouble(valueList.get(i));
		}
		throw new RuntimeException("StatContainer has no such key: "+key);
	}
	
	public String getString( String key ) {
		for ( int i=0; i<primaryNameList.size(); ++i ) {
			if ( primaryNameList.get(i).equals(key) ) return primaryValueList.get(i);
		}
		for ( int i=0; i<nameList.size(); ++i ) {
			if ( nameList.get(i).equals(key) ) return valueList.get(i);
		}
		throw new RuntimeException("StatContainer has no such key: "+key);
	}

	public String getLegend( int[] primarykeyblank, int[] keyblank ) {
		final StringBuilder legendBuilder = new StringBuilder();
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

	public void printResult() {
		int maxKeyLength = 0;
		for( int i = 0; i < primaryNameList.size(); i++ ) {
			final int length = primaryNameList.get( i ).length();

			if( maxKeyLength < length ) {
				maxKeyLength = length;
			}
		}

		for( int i = 0; i < nameList.size(); i++ ) {
			final int length = nameList.get( i ).length();

			if( maxKeyLength < length ) {
				maxKeyLength = length;
			}
		}

		for( int i = 0; i < primaryNameList.size(); i++ ) {
			final int length = primaryNameList.get( i ).length();
			System.out.print( primaryNameList.get( i ) );

			for( int f = length; f < maxKeyLength; f++ ) {
				System.out.print( ' ' );
			}

			System.out.print( "  " );
			System.out.println( primaryValueList.get( i ) );
		}

		for( int i = 0; i < nameList.size(); i++ ) {
			final int length = nameList.get( i ).length();
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
			catch( final FileNotFoundException e1 ) {
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
			catch( final Exception e ) {
				prevLegend = "";
			}
			try {
				br.close();
			}
			catch( final IOException e ) {
				e.printStackTrace();
			}
		}

		PrintStream dataFile = null;
		try {
			dataFile = new PrintStream( new FileOutputStream( filename, true ) );
		}
		catch( final FileNotFoundException e1 ) {
			e1.printStackTrace();
			return;
		}

		final int[] primarykeyblank = new int[ primaryNameList.size() ];
		final int[] primaryvalueblank = new int[ primaryValueList.size() ];
		for( int i = 0; i < primaryNameList.size(); i++ ) {

			String name = primaryNameList.get( i );
			String value = primaryValueList.get( i );

			if( name.contains( "Path" ) && !name.contains( "output" ) && value.length() < 28 ) {
				primarykeyblank[ i ] = 28 - name.length() - 2;
				primaryvalueblank[ i ] = 28 - value.length();
			}
			else {
				primarykeyblank[ i ] = value.length() - name.length() - 2;
				primaryvalueblank[ i ] = name.length() + 2 - value.length();
			}
		}
		// due to #
		primarykeyblank[ 0 ] -= 1;

		final int[] keyblank = new int[ nameList.size() ];
		final int[] valueblank = new int[ valueList.size() ];
		for( int i = 0; i < nameList.size(); i++ ) {
			keyblank[ i ] = valueList.get( i ).length() - nameList.get( i ).length() - 2;
			valueblank[ i ] = nameList.get( i ).length() + 2 - valueList.get( i ).length();
		}

		final String legend = getLegend( primarykeyblank, keyblank );

		if( !legend.equals( prevLegend ) ) {
			// print legend
			try {
				dataFile.println( legend );
			}
			catch( final Exception e ) {
				e.printStackTrace();
			}
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
		catch( final Exception e ) {
			e.printStackTrace();
		}
		finally {
			dataFile.close();
		}
	}

	private static void appendBlank( PrintStream dataFile, int blankCount ) {
		if( blankCount < 0 ) {
			return;
		}
		for( int i = 0; i < blankCount; i++ ) {
			dataFile.print( ' ' );
		}
	}

	private static void appendBlank( StringBuilder builder, int blankCount ) {
		if( blankCount < 0 ) {
			return;
		}
		for( int i = 0; i < blankCount; i++ ) {
			builder.append( ' ' );
		}
	}

	public String toJson() {
		final StringBuilder bld = new StringBuilder();

		for( int i = 0; i < primaryNameList.size(); i++ ) {
			if( i != 0 ) {
				bld.append( "," );
			}
			bld.append( "\"" + primaryNameList.get( i ).replaceAll( "\"", "" ) + "\":" );
			bld.append( "\"" + primaryValueList.get( i ).replaceAll( "\"", "" ) + "\"" );
		}

		for( int i = 0; i < nameList.size(); i++ ) {
			if( i != 0 || ( primaryNameList.size() != 0 ) ) {
				bld.append( "," );
			}
			bld.append( "\"" + nameList.get( i ) + "\":" );
			bld.append( "\"" + valueList.get( i ).replaceAll( "\"", "\\\\\"" ) + "\"" );
		}

		return bld.toString();
	}
	
	public void setPrimaryValue( String key, String value ) {
		for ( int i=0; i< this.primaryNameList.size(); ++i ) {
			String name = this.primaryNameList.get( i );
			if ( name.equals(key) ) {
				this.primaryValueList.set(i, value);
				return;
			}
		}
		this.primaryNameList.add(key);
		this.primaryValueList.add(value);
	}
	
	public void merge( StatContainer statOther, Collection<IntegerPair> rslt ) {
		// build primary(Name,Value)List
		for ( int i=0; i< this.primaryNameList.size(); ++i ) {
			String name = this.primaryNameList.get( i );
			String value = this.primaryValueList.get( i );
//			if ( name.equals( "Final_Result_Size" ) ) this.primaryValueList.set( i, String.valueOf( rslt.size() ) );
//			System.out.println( "\t"+name+" : "+value );
		}
		
		// build secondary(Name,Value)List
		Object2ObjectOpenHashMap<String, String> mapStat2 = new Object2ObjectOpenHashMap<>();
		for ( int i=0; i< statOther.nameList.size(); ++i ) {
			String name = statOther.nameList.get( i );
			String value = statOther.valueList.get( i );
			mapStat2.put( name, value );
		}

		for ( int i=0; i< this.nameList.size(); ++i ) {
			String name = this.nameList.get( i );
			String value = this.valueList.get( i );
			if ( name.startsWith( "cmd_" ) || !mapStat2.containsKey( name ) ) {}
			else if ( name.startsWith( "Mem_" ) ) {
				String value2 = mapStat2.get( name );
				value = String.valueOf( Long.max( Long.parseLong( value ),Long.parseLong( value2 ) ) );
			}
			else {
				String value2 = mapStat2.get( name );
				try { value = String.valueOf( Long.parseLong( value ) + Long.parseLong( value2 ) ); }
				catch ( NumberFormatException e ) {
					try { value = String.valueOf( Double.parseDouble( value ) + Double.parseDouble( value2 ) ); }
					catch ( NumberFormatException e2 ) {}
				}
			}
			this.valueList.set( i, value );
		}
	}

	public static StatContainer merge( StatContainer stat1, StatContainer stat2 ) {
		StatContainer stat = new StatContainer();

		// build primary(Name,Value)List
		for ( int i=0; i< stat1.primaryNameList.size(); ++i ) {
			String name1 = stat1.primaryNameList.get( i );
			String value1 = stat1.primaryValueList.get( i );
			String name2 = stat2.primaryNameList.get( i );
			String value2 = stat2.primaryValueList.get( i );
			if ( !name1.equals(name2) ) throw new RuntimeException(name1+", "+name2);
			String value = value1;
			try { value = String.valueOf( Long.parseLong(value1) + Long.parseLong(value2) ); }
			catch ( NumberFormatException e ) {}
			stat.primaryNameList.add(name1);
			stat.primaryValueList.add(value);
		}
		
		// build secondary(Name,Value)List
		Object2ObjectOpenHashMap<String, String> mapStat2 = new Object2ObjectOpenHashMap<>();
		for ( int i=0; i< stat2.nameList.size(); ++i ) {
			String name = stat2.nameList.get( i );
			String value = stat2.valueList.get( i );
			mapStat2.put( name, value );
		}

		for ( int i=0; i< stat1.nameList.size(); ++i ) {
			String name = stat1.nameList.get( i );
			String value = stat1.valueList.get( i );
			if ( !mapStat2.containsKey( name ) ) {}
			else if ( name.startsWith("alg") || name.startsWith("Param_") ) {}
			else if ( name.startsWith( "Mem_" ) ) {
				String value2 = mapStat2.get( name );
				value = String.valueOf( Long.max( Long.parseLong( value ),Long.parseLong( value2 ) ) );
			}
			else {
				String value2 = mapStat2.get( name );
				try { value = String.valueOf( Long.parseLong( value ) + Long.parseLong( value2 ) ); }
				catch ( NumberFormatException e ) {
					try { value = String.valueOf( Double.parseDouble( value ) + Double.parseDouble( value2 ) ); }
					catch ( NumberFormatException e2 ) {}
				}
			}
			stat.nameList.add(name);
			stat.valueList.add(value);
		}
		
		return stat;
	}
}
