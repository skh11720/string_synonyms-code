package analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Statistics {
	public static void main( String[] args ) throws IOException {
		BufferedReader br = new BufferedReader( new FileReader( args[ 0 ] ) );
		String line;
		Map<String, Integer> freq = new HashMap<String, Integer>();
		while( ( line = br.readLine() ) != null ) {
			String[] pline = line.split( "[ ,\t]+" );
			for( String t : pline )
				increase( freq, t );
		}
		br.close();
		List<StringIntPair> freqs = new ArrayList<StringIntPair>();
		for( Entry<String, Integer> e : freq.entrySet() ) {
			freqs.add( new StringIntPair( e.getKey(), e.getValue() ) );
		}
		Collections.sort( freqs );
		Collections.reverse( freqs );

		System.out.println( "Distinct tokens: " + freq.size() );
		System.out.println( "Max freq: " + freqs.get( 0 ).i );
		System.out.println( "Min freq: " + freqs.get( freqs.size() - 1 ).i );

		BufferedWriter bw = new BufferedWriter( new FileWriter( "asdf" ) );
		for( StringIntPair p : freqs )
			bw.write( p.i + "\t" + p.str + "\n" );
		bw.close();
	}

	private static void increase( Map<String, Integer> freq, String key ) {
		Integer prev_val = freq.get( key );
		if( prev_val == null )
			freq.put( key, 1 );
		else
			freq.put( key, prev_val + 1 );
	}

	private static class StringIntPair implements Comparable<StringIntPair> {
		String str;
		int i;

		StringIntPair( String str, int i ) {
			this.str = str;
			this.i = i;
		}

		public int compareTo( StringIntPair o ) {
			int cmp = Integer.compare( i, o.i );
			if( cmp != 0 )
				return cmp;
			return str.compareTo( o.str );
		}
	}
}
