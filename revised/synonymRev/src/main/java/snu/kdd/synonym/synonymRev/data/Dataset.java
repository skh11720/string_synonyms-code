package snu.kdd.synonym.synonymRev.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Dataset {
	String name;
	ObjectArrayList<Record> recordList;
	int nRecord;

	public Dataset( String dataFile, TokenIndex tokenIndex ) throws IOException {
		BufferedReader br = new BufferedReader( new FileReader( dataFile ) );
		this.name = dataFile;

		String line;
		recordList = new ObjectArrayList<>();

		nRecord = 0;
		while( ( line = br.readLine() ) != null ) {
			recordList.add( new Record( nRecord++, line, tokenIndex ) );
		}
		br.close();
	}

	public Iterable<Record> get() {
		return recordList;
	}

	public Record getRecord( int id ) {
		return recordList.get( id );
	}

	public int size() {
		return nRecord;
	}
}
