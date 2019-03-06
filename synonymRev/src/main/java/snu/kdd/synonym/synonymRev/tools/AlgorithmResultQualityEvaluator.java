package snu.kdd.synonym.synonymRev.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmStatInterface;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;

public class AlgorithmResultQualityEvaluator {
	
	private int tp = 0;
	private int fp = 0;
	private int fn = 0;
	private final String outputPath;
	private final Dataset searchedSet;
	private final Dataset indexedSet;

	public static void evaluate( AlgorithmInterface alg, Query query, String groundPath ) {
		if (groundPath == null ) {
			System.err.println( "-groundPath option is not given. The quality evaluation skipped.");
		}
		else {
			AlgorithmResultQualityEvaluator evaluator = new AlgorithmResultQualityEvaluator(alg, query, groundPath);
			alg.getStat().add(AlgorithmStatInterface.EVAL_TP, evaluator.tp);
			alg.getStat().add(AlgorithmStatInterface.EVAL_FP, evaluator.fp);
			alg.getStat().add(AlgorithmStatInterface.EVAL_FN, evaluator.fn);
			alg.getStat().add(AlgorithmStatInterface.EVAL_PRECISION, evaluator.getPrecision());
			alg.getStat().add(AlgorithmStatInterface.EVAL_RECALL, evaluator.getRecall());
			alg.getStat().add(AlgorithmStatInterface.EVAL_F1SCORE, evaluator.getF1score());
		}
	}
	
	private AlgorithmResultQualityEvaluator( AlgorithmInterface alg, Query query, String groundPath ) {
		outputPath = "./tmp/EVAL_" + alg.getNameWithParam() + "_" + query.dataInfo.datasetName;
		searchedSet = query.searchedSet;
		indexedSet = query.indexedSet;
		Set<IntegerPair> rslt = alg.getResult();
		Set<IntegerPair> groundSet = getGroundTruthSet(groundPath);
		compareIntPairSetsWrapper(groundSet, rslt);
	}

	private Set<IntegerPair> getGroundTruthSet( String groundPath ) {
		ObjectOpenHashSet<IntegerPair> groundSet = new ObjectOpenHashSet<>();
		try {
			BufferedReader br = new BufferedReader( new FileReader(groundPath) 	);
			for (String line = null; (line = br.readLine()) != null; ) {
				String[] token = line.trim().split("\\s+");
				int i1 = Integer.parseInt(token[0]);
				int i2 = Integer.parseInt(token[1]);
				groundSet.add( new IntegerPair(i1, i2) );
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
	
	public double getF1score() {
		double p = getPrecision();
		double r = getRecall();
		if (p+r == 0) return 0.0;
		else return 2*p*r/(p+r);
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
