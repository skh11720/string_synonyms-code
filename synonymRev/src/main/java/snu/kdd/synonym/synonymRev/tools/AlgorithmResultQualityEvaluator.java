package snu.kdd.synonym.synonymRev.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;

public class AlgorithmResultQualityEvaluator {
	
	private final int tp;
	private final int fp;
	private final int fn;

	public static void evaluate( AlgorithmInterface alg, String groundPath ) {
		if (groundPath == null ) {
			System.err.println( "-groundPath option is not given. The quality evaluation skipped.");
		}
		else {
			/*
			 *  the ipairs in "rslt" are assumed to be ordered (i1 <= i2). 
			 *  if something is wrong, check them in "rslt".
			 */
			Set<IntegerPair> rslt = alg.getResult();
			Set<IntegerPair> groundSet = getGroundTruthSet(groundPath);

			AlgorithmResultQualityEvaluator evaluator = new AlgorithmResultQualityEvaluator(groundSet, rslt);
			alg.getStat().add("Eval_TP", evaluator.tp);
			alg.getStat().add("Eval_FP", evaluator.fp);
			alg.getStat().add("Eval_FN", evaluator.fn);
			alg.getStat().add("Eval_Precision", evaluator.getPrecision());
			alg.getStat().add("Eval_Recall", evaluator.getRecall());
		}
	}
	
	private static Set<IntegerPair> getGroundTruthSet( String groundPath ) {
		ObjectOpenHashSet<IntegerPair> groundSet = new ObjectOpenHashSet<>();
		try {
			BufferedReader br = new BufferedReader( new FileReader(groundPath) 	);
			for (String line = null; (line = br.readLine()) != null; ) {
				int[] intPairArr = Arrays.stream( line.trim().split("\\s+") ).mapToInt( Integer::parseInt ).toArray();
				groundSet.add( new IntegerPair(intPairArr).ordered() );
			}
			br.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return groundSet;
	}
	
	private AlgorithmResultQualityEvaluator( Set<IntegerPair> groundSet, Set<IntegerPair> rslt ) {
		int tp = 0;
		int fp = 0;
		int fn = 0;
		for ( final IntegerPair ipair : groundSet ) {
			if ( ipair.i1 == ipair.i2 ) continue;
			if ( rslt.contains(ipair) ) ++tp;
			else ++fn;
		}
		for ( final IntegerPair ipair : rslt ) {
			if ( ipair.i1 == ipair.i2 ) continue;
			if ( !groundSet.contains(ipair) ) ++fp;
		}
		this.tp = tp;
		this.fp = fp;
		this.fn = fn;
	}
	
	private double getPrecision() {
		return 1.0*tp/(tp + fp);
	}
	
	private double getRecall() {
		return 1.0*tp/(tp + fn);
	}
}
