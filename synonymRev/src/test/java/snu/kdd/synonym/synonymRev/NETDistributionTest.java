package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;

import org.junit.Test;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;

public class NETDistributionTest {

	static final String[] datasetArray = {"AOL", "SPROT", "USPS"};
	static final int[] sizeArray = {10000, 15848, 25118, 39810, 63095, 100000};

	@Test
	public void test() throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/NETDistributionTest.txt")));
		int size = 10000;
		double[][] val = new double[size][3];
		
		for ( int j=0; j<3; ++j ) {
			String dataset = datasetArray[j];
			Query query = Util.getQueryWithPreprocessing(dataset, size);
			RecordComparator comp = new RecordComparator();
			Collections.sort(query.searchedSet.recordList, comp);

			for ( int i=0; i<query.searchedSet.size(); ++i ) 
				val[i][j] = query.searchedSet.getRecord(i).getEstNumTransformed();
		}		

		for ( int i=0; i<size; ++i ) {
			for ( int j=0; j<3; ++j )
				pw.print(val[i][j]+"\t");
			pw.println();
		}
		pw.flush();
		pw.close();
	}

	class RecordComparator implements Comparator<Record> {
		@Override
		public int compare( Record o1, Record o2 ) {
			long est1 = o1.getEstNumTransformed();
			long est2 = o2.getEstNumTransformed();
			return Long.compare( est1, est2 );
		}
	};
}
