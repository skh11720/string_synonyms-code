package snu.kdd.synonym.synonymRev.estimation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.Validator;

// Refactoring code for estimate

public class SampleEstimateByRegression extends SampleEstimate {
	
	private final int num_split = 5;
	private long[] mh_term3_split, min_term3_split;
	IntArrayList idxArray;
	int[] sizeList;
//	SelectivityEstimator selest_mh, selest_min;
	double est_mh_term3, est_min_term3;
	double[][] x;

	public SampleEstimateByRegression( final Query query, double sampleRatio, boolean isSelfJoin ) {
		super(query, sampleRatio, isSelfJoin );
		/*
		 * sampleSearchedList and sampledIndexedList are sorted.
		 */
		mh_term3_split = new long[num_split];
		Arrays.fill( mh_term3_split, 0 );
		min_term3_split = new long[num_split];
		Arrays.fill( min_term3_split, 0 );
		
		// shuffle sampleSearchedList
		idxArray = new IntArrayList();
		sizeList = new int[num_split];
		Arrays.fill( sizeList, 0 );
		for ( int i=0; i<sampleSearchedSize; ++i ) idxArray.add( i );
		Collections.shuffle( idxArray );
		for ( int i=0; i<sampleSearchedSize; ++i ) ++sizeList[i % num_split];
		for ( int j=1; j<num_split; ++j ) sizeList[j] += sizeList[j-1];

		x = new double[num_split][];
		for ( int j=0; j<num_split; ++j ) x[j] = new double[] {sizeList[j], sizeList[j]*sizeList[j]};
	}

	@Override
	protected long sampleJoinMH( JoinMHIndex joinmhinst, Validator checker ) {
		long joinTime = super.sampleJoinMH( joinmhinst, checker );
		
		long[] _mh_term3 = new long[sampleSearchedSize];
		_mh_term3[0] = mh_term3[0];
		for ( int i=1; i<sampleSearchedSize; ++i ) _mh_term3[i] = mh_term3[i] - mh_term3[i-1];
		for ( int i=0; i<sampleSearchedSize; ++i ) {
			int idx = idxArray.getInt( i );
			int split_idx = i % num_split;
			mh_term3_split[split_idx] += _mh_term3[idx];
		}
		for ( int j=1; j<num_split; ++j ) mh_term3_split[j] += mh_term3_split[j-1];
		
		// conduct the regression
		double[] y = new double[num_split];
		for ( int j=0; j<num_split; ++j ) y[j] = mh_term3_split[j];

//		for ( int j=0; j<num_split; ++j ) {
//			System.out.println( j+"\t"+Arrays.toString( x[j] )+"\t"+y[j] );
//		}
		SelectivityEstimator selest_mh = new SelectivityEstimator( x, y );
		est_mh_term3 = selest_mh.predict( new double[] { query.searchedSet.size(), query.searchedSet.size()*query.searchedSet.size()} );
		
		return joinTime;
	}
	
	@Override
	protected long sampleJoinMin( JoinMinIndex joinmininst, Validator checker, int indexK ) {
		long joinTime = super.sampleJoinMin( joinmininst, checker, indexK );

		long[] _min_term3 = new long[sampleSearchedSize];
		_min_term3[0] = min_term3[0];
		for ( int i=1; i<sampleSearchedSize; ++i ) _min_term3[i] = min_term3[i] - min_term3[i-1];
		for ( int i=0; i<sampleSearchedSize; ++i ) {
			int idx= idxArray.getInt( i );
			int split_idx = i % num_split;
			min_term3_split[split_idx] += _min_term3[idx];
		}
		for ( int j=1; j<num_split; ++j ) min_term3_split[j] += min_term3_split[j-1];
		
		// conduct the regression
		double[] y = new double[num_split];
		for ( int j=0; j<num_split; ++j ) y[j] = min_term3_split[j];
		
		SelectivityEstimator selest_min = new SelectivityEstimator( x, y );
		est_min_term3 = selest_min.predict( new double[] { query.searchedSet.size(), query.searchedSet.size()*query.searchedSet.size()} );

		return joinTime;
	}
	public Object2DoubleMap<String> estimateJoinMin( StatContainer stat, Validator checker, int indexK, int qSize ) {
		Object2DoubleMap<String> output = super.estimateJoinMin( stat, checker, indexK, qSize );

		
		return output;
	}

	@Override
	public double getEstimateJoinMH( double term1, double term2, double term3 ) {
		return coeff_mh_1 * term1 / sampleRatio 
				+ coeff_mh_2 * term2 / sampleRatio
				+ coeff_mh_3 * est_mh_term3 / mh_term3[sampleSearchedSize-1] * term3;
	}

	@Override
	public double getEstimateJoinMin( double term1, double term2, double term3 ) {
		return coeff_min_1 * term1 / sampleRatio 
				+ coeff_min_2 * term2 / sampleRatio
				+ coeff_min_3 * est_min_term3 / min_term3[sampleSearchedSize-1] * term3;
	}
}