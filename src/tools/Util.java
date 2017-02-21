package tools;

import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class Util {

	public static void printArgsError( CommandLine cmd ) {
		Iterator<Option> itr = cmd.iterator();

		int maxSize = 0;
		while( itr.hasNext() ) {
			Option opt = itr.next();
			int size = opt.getOpt().length();
			if( maxSize < size ) {
				maxSize = size;
			}
		}
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
	}

}
