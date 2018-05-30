package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;

public interface PQFilterIndexInterface {

	public Map<QGram, List<Record>> get( int pos );
	
	public Iterable<Integer> getPosSet();
	
	public int getIndexedCount( Record rec );

	public void writeToFile();
}
