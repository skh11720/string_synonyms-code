package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinFast;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.SetGreedyOneSide;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.SetNaiveOneSide;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.SetTopDownOneSide;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinFastIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.NaiveOneSide;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class ValidatorSpeedTest {

	static final int K = 1;
	static final int q = 2;

	@Test
	public void test() throws IOException, ParseException, org.json.simple.parser.ParseException {
		String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K"};
//		String[] datasetList = {"SYN_100K"};
//		String[] attrList = {"Val_Comparisons", "Val_Length_filtered", "Val_PQGram_filtered"};
		int[] datasetSizeList = {1000000, 466158, 1000000, 1000000};
		int nSample = 1000;
		Random rand = new Random();
		double[][] result = new double[datasetList.length][5];

		PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/ValidatorSpeedTest.txt" ) ) );
		writer.println("dataset\tsize\tnSample\tavg_time");
		

		for ( int idx_dataset=0; idx_dataset<datasetList.length; ++idx_dataset ) {
			String dataset = datasetList[idx_dataset];
			int size = datasetSizeList[idx_dataset];
			Query query = TestUtils.getTestQuery( dataset, size );
			int[] arr_sidx = getSampleRecordsIdxArray( query.searchedSet.recordList, nSample, rand, true );
			int[] arr_tidx = getSampleRecordsIdxArray( query.indexedSet.recordList, nSample, rand, false );

			boolean isSelfJoin = true;
			if ( dataset.equals( "SYN_100K" ) ) isSelfJoin = false;
			NaiveOneSide valid_seq_n = new NaiveOneSide();
			TopDownOneSide valid_seq_dp = new TopDownOneSide();
			SetNaiveOneSide valid_set_n = new SetNaiveOneSide( isSelfJoin );
			SetTopDownOneSide valid_set_dp = new SetTopDownOneSide( isSelfJoin );
			SetGreedyOneSide valid_set_gd = new SetGreedyOneSide( isSelfJoin, 1 );
			
			Validator[] arr_validator = {valid_seq_n, valid_seq_dp, valid_set_n, valid_set_dp, valid_set_gd, };
			for ( int idx_validator=0; idx_validator<arr_validator.length; ++idx_validator ) {
				Validator validator = arr_validator[idx_validator];
				long ts = System.nanoTime();

//				for ( int idx_sample=0; idx_sample<nSample; ++idx_sample ) {
//					Record recS = query.searchedSet.getRecord(arr_sidx[idx_sample]);
//					Record recT = query.indexedSet.getRecord(arr_tidx[idx_sample]);
//					validator.isEqual( recS, recT );
//				}
//				double avg_t = (System.nanoTime() - ts)/1e3/nSample; // time unit: us

				for ( int i=0; i<arr_sidx.length; ++i ) {
					Record recS = query.searchedSet.getRecord(i);
					for ( int j=0; j<arr_tidx.length; ++j ) {
						Record recT = query.indexedSet.getRecord(j);
						validator.isEqual( recS, recT );
					}
				}
				double avg_t = (System.nanoTime() - ts)/1e3/nSample/nSample; // time unit: us

				result[idx_dataset][idx_validator] = avg_t;
				System.out.println( dataset+"\t"+size+"\t"+nSample+"\t"+avg_t );
				writer.println( dataset+"\t"+size+"\t"+nSample+"\t"+avg_t );
			}
		}
		
		/*
		 * print result to be added in the paper.
		 * a row = validator
		 * a column = dataset
		 */
		writer.println();
		String[] arr_str_valid = {"valid_seq_n", "valid_seq_dp", "valid_set_n", "valid_set_dp", "valid_set_gd"};
		for ( String dataset : datasetList ) writer.print("\t"+dataset);
		writer.println();
		for ( int j=0; j<arr_str_valid.length; ++j ) {
			writer.print( arr_str_valid[j] );
			for ( int i=0; i<datasetList.length; ++i ) {
				writer.print( String.format( "\t%.3f", result[i][j] ) ); 
			}
			writer.println();
		}
		
		writer.flush(); writer.close();
	}

	private int[] getSampleRecordsIdxArray( List<Record> recordList, int nSample, Random rand, boolean useThreshold ) {
		IntOpenHashSet idxSet = new IntOpenHashSet();
		while ( idxSet.size() < nSample ) {
			int idx = rand.nextInt( recordList.size() );
			if ( idxSet.contains( idx ) ) continue;
			Record rec = recordList.get( idx );
//			if ( useThreshold && rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			if ( useThreshold && rec.getEstNumTransformed() > 10000 ) continue;
			idxSet.add( idx );
		}
		return idxSet.toIntArray();
	}
}
