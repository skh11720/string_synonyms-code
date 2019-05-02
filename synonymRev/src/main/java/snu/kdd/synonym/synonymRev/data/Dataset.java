package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Dataset {
	public final String path;
	public ObjectArrayList<Record> recordList;

	public Dataset( String dataPath, TokenIndex tokenIndex ) throws IOException {
		this.path = dataPath;
		this.recordList = new ObjectArrayList<>();

		BufferedReader br = new BufferedReader( new FileReader( dataPath ) );
		String line;
		for ( int i=0; ( line = br.readLine() ) != null; ++i ) {
			this.recordList.add( new Record( i, line, tokenIndex ) );
		}
		br.close();
	}

	public Dataset( ObjectArrayList<Record> record ) {
		this.path = null;
		this.recordList = record;
	}

	public Iterable<Record> get() {
		return this.recordList;
	}

	public Record getRecord( int id ) {
		return this.recordList.get( id );
	}

	public int size() {
		return this.recordList.size();
	}
}
