package snu.kdd.synonym.synonymRev.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmStatInterface;
import snu.kdd.synonym.synonymRev.data.Dataset;

public class AlgorithmResultQualityEvaluator {
	
	private int tp = 0;
	private int fp = 0;
	private int fn = 0;
	private final String outputPath;
	private final Dataset searchedSet;
	private final Dataset indexedSet;

	public static void evaluate( AlgorithmInterface alg, String groundPath ) {
		if (groundPath == null ) {
			System.err.println( "-groundPath option is not given. The quality evaluation skipped.");
		}
		else {
			AlgorithmResultQualityEvaluator evaluator = new AlgorithmResultQualityEvaluator(alg, groundPath);
			alg.getStat().add(AlgorithmStatInterface.EVAL_TP, evaluator.tp);
			alg.getStat().add(AlgorithmStatInterface.EVAL_FP, evaluator.fp);
			alg.getStat().add(AlgorithmStatInterface.EVAL_FN, evaluator.fn);
			alg.getStat().add(AlgorithmStatInterface.EVAL_PRECISION, evaluator.getPrecision());
			alg.getStat().add(AlgorithmStatInterface.EVAL_RECALL, evaluator.getRecall());
		}
	}
	
	private AlgorithmResultQualityEvaluator( AlgorithmInterface alg, String groundPath ) {
		outputPath = "./tmp/EVAL_" + alg.getQuery().dataInfo.getName();
		searchedSet = alg.getQuery().searchedSet;
		indexedSet = alg.getQuery().indexedSet;
		Set<IntegerPair> rslt = alg.getResult();
		Set<IntegerPair> groundSet = getGroundTruthSet(groundPath);
		compareIntPairSetsWrapper(groundSet, rslt);
	}

	private Set<IntegerPair> getGroundTruthSet( String groundPath ) {
		ObjectOpenHashSet<IntegerPair> groundSet = new ObjectOpenHashSet<>();
		try {
			BufferedReader br = new BufferedReader( new FileReader(groundPath) 	);
			for (String line = null; (line = br.readLine()) != null; ) {
				int[] intPairArr = Arrays.stream( line.trim().split("\\s+") ).mapToInt( Integer::parseInt ).toArray();
				groundSet.add( new IntegerPair(intPairArr) );
			}
			br.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return groundSet;
	}

	private void compareIntPairSetsWrapper( Set<IntegerPair> groundSet, Set<IntegerPair> rslt ) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(outputPath)));
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		compareIntPairSets(groundSet, rslt, pw);
		pw.close();
	}
	
	private void compareIntPairSets( Set<IntegerPair> groundSet, Set<IntegerPair> rslt, PrintWriter pw ) {
		/*	
		 * For the case of self joins, ipairs in the both sets are assumed to be orderd (i1 <= i2). 
		 *  if something is wrong, check it out.
		 */
		for ( final IntegerPair ipair : groundSet ) {
			if ( ipair.i1 == ipair.i2 ) continue;
			if ( rslt.contains(ipair) ) {
				if ( pw != null ) pw.println("TP\t"+getRecordPairStringFromIntPair(ipair));
				++tp;
			}
			else {
				if ( pw != null ) pw.println("FN\t"+getRecordPairStringFromIntPair(ipair));
				++fn;
			}
		}
		for ( final IntegerPair ipair : rslt ) {
			if ( ipair.i1 == ipair.i2 ) continue;
			if ( !groundSet.contains(ipair) ) {
				if ( pw != null ) pw.println("FP\t"+getRecordPairStringFromIntPair(ipair));
				++fp;
			}
		}
	}
	
	private String getRecordPairStringFromIntPair(IntegerPair ipair) {
		return String.format("%d\t%s\t%d\t%s", 
				ipair.i1, searchedSet.getRecord(ipair.i1),
				ipair.i2, indexedSet.getRecord(ipair.i2));
	}
	
	public double getPrecision() {
		if (tp + fp == 0) return 0.0;
		else return 1.0*tp/(tp + fp);
	}
	
	public double getRecall() {
		if (tp + fn == 0) return 0.0;
		else return 1.0*tp/(tp + fn);
	}
	
	public int getTP() {
		return tp;
	}
	
	public int getFP() {
		return fp;
	}
	
	public int getFN() {
		return fn;
	}
}
