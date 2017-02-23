package snu.kdd.synonym.tools;

import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class Util {

	public static void printLog( String message ) {
		System.out.println( toLogString( message ) );
	}

	public static String toLogString( String message ) {
		// print log messages
		StackTraceElement[] tr = new Throwable().getStackTrace();
		String className = tr[ 2 ].getClassName();

		final StringBuilder bld = new StringBuilder();
		bld.append( new java.text.SimpleDateFormat( "yyyy/MM/dd HH:mm:ss " ).format( new java.util.Date() ) );
		bld.append( '[' );
		bld.append( className.substring( className.lastIndexOf( '.' ) + 1, className.length() ) );
		bld.append( '.' );
		bld.append( tr[ 2 ].getMethodName() );
		bld.append( ']' );
		bld.append( ' ' );
		bld.append( message );
		return bld.toString();
	}

	public static void printArgsError( CommandLine cmd ) {
		Iterator<Option> itr = cmd.iterator();

		int maxRowSize = 0;
		int maxSize = 0;
		while( itr.hasNext() ) {
			Option opt = itr.next();
			int size = opt.getOpt().length();
			if( maxSize < size ) {
				maxSize = size;
			}

			if( opt.getValue() != null ) {
				size += opt.getValue().length() + 3;
			}

			if( maxRowSize < size ) {
				maxRowSize = size;
			}
		}

		for( int i = 0; i < ( maxRowSize - 14 ) / 2; i++ ) {
			System.out.print( "=" );
		}
		System.out.print( "[printArgsError]" );
		for( int i = 0; i < ( maxRowSize - 14 ) - 1 / 2; i++ ) {
			System.out.print( "=" );
		}
		System.out.println( "=" );

		itr = cmd.iterator();
		while( itr.hasNext() ) {
			Option opt = itr.next();
			int size = opt.getOpt().length();
			System.err.print( opt.getOpt() );
			for( int i = size; i < maxSize; i++ ) {
				System.err.print( " " );
			}
			System.err.println( " : " + opt.getValue() );
		}

		for( int i = 0; i < maxRowSize - 1; i++ ) {
			System.out.print( "=" );
		}
		System.out.println( "=" );
	}

}
