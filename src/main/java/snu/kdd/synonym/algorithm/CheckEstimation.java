package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

import mine.Record;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.estimation.CountEntry;
import snu.kdd.synonym.estimation.SampleEstimate;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import tools.DEBUG;
import validator.Validator;

public class CheckEstimation extends AlgorithmTemplate {

	// private RecordIDComparator idComparator;
	// private RuleTrie ruletrie;

	private DataInfo dataInfo;
	private Validator checker;

	private SampleEstimate estimate;

	private int qSize = -1;

	private double totalExpLengthNaiveIndex = 0;
	private double totalExpNaiveJoin = 0;

	private long maxSearchedEstNumRecords;
	private long maxIndexedEstNumRecords;

	int joinThreshold = 0;
	boolean joinMinRequired = true;

	public CheckEstimation( String rulefile, String Rfile, String Sfile, String outputPath, DataInfo info, boolean oneSideJoin,
			StatContainer stat ) throws IOException {
		super( rulefile, Rfile, Sfile, outputPath, info, oneSideJoin, stat );
		// idComparator = new RecordIDComparator();
		// ruletrie = new RuleTrie( rulelist );

		this.dataInfo = info;
	}

	@Override
	public void run( String[] args ) {
		Param params = Param.parseArgs( args, stat );
		// Setup parameters
		checker = params.getValidator();
		qSize = params.getQGramSize();

		run( params.getSampleRatio() );
		Validator.printStats();
	}

	public void run( double sampleratio ) {
		preprocess();

		// Estimate constants
		findConstants( sampleratio );

		estimate.findThetaUnrestrictedDebug( qSize, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords, oneSideJoin );

		System.out.println( "----------------------------------------------------" );
		System.out.println( "OneSideJoin: " + oneSideJoin );
		joinThreshold = estimate.findThetaUnrestrictedCountAll( qSize, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
				oneSideJoin );
		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMinRequired = false;
		}
	}

	protected void preprocess() {
		super.preprocess( true, -1, false );

		// Sort R and S with expanded sizes
		Comparator<Record> cmp = new Comparator<Record>() {
			@Override
			public int compare( Record o1, Record o2 ) {
				long est1 = o1.getEstNumRecords();
				long est2 = o2.getEstNumRecords();
				return Long.compare( est1, est2 );
			}
		};

		long sortTime;
		if( DEBUG.JoinHybridON ) {
			sortTime = System.currentTimeMillis();
		}

		Collections.sort( tableSearched, cmp );
		Collections.sort( tableIndexed, cmp );

		if( DEBUG.JoinHybridON ) {
			stat.add( "Result_2_7_Preprocess_Sorting_Time", System.currentTimeMillis() - sortTime );
		}

		// Reassign ID and collect statistics for join naive
		int currentIdx = 0;
		int nextThreshold = 10;

		for( int i = 0; i < tableSearched.size(); ++i ) {
			Record t = tableSearched.get( i );
			t.setID( i );

			long est = t.getEstNumRecords();
			totalExpNaiveJoin += est;

			while( currentIdx != CountEntry.countMax - 1 && est >= nextThreshold ) {
				nextThreshold *= 10;
				currentIdx++;
			}
		}

		currentIdx = 0;
		nextThreshold = 10;
		for( int i = 0; i < tableIndexed.size(); ++i ) {
			Record s = tableIndexed.get( i );
			s.setID( i );

			long est = s.getEstNumRecords();
			double estLength = (double) est * (double) s.getTokenArray().length;
			totalExpLengthNaiveIndex += estLength;

			while( currentIdx != CountEntry.countMax - 1 && s.getEstNumRecords() >= nextThreshold ) {
				nextThreshold *= 10;
				currentIdx++;
			}
		}

		maxSearchedEstNumRecords = tableSearched.get( tableSearched.size() - 1 ).getEstNumRecords();
		maxIndexedEstNumRecords = tableIndexed.get( tableIndexed.size() - 1 ).getEstNumRecords();

		stat.add( "Preprocess_ExpLength_Total", totalExpLengthNaiveIndex );
		stat.add( "Preprocess_Exp_Total", totalExpNaiveJoin );
	}

	private void findConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate( tableSearched, tableIndexed, sampleratio, dataInfo.isSelfJoin() );
		estimate.estimateWithSample( stat, this, checker, qSize );
	}

	@Override
	public String getName() {
		return "CheckEstimation";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}
