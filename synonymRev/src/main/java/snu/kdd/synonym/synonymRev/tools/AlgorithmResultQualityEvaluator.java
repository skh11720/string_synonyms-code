package snu.kdd.synonym.synonymRev.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmStatInterface;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class AlgorithmResultQualityEvaluator {
	
	private int tp = 0;
	private int fp = 0;
	private int fn = 0;
	private final String outputPath;
	private final Dataset searchedSet;
	private final Dataset indexedSet;
	private static Validator checker = new TopDownOneSide();

	public static void evaluate( AlgorithmInterface alg, Query query, String groundPath ) {
		if (groundPath == null ) {
			System.out.println( "-groundPath option is not given. The quality evaluation skipped.");
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
		ResultSet rslt = alg.getResult();
		ResultSet groundSet = getGroundTruthSet(groundPath);
		compareIntPairSetsWrapper(groundSet, rslt, query);
	}

	private ResultSet getGroundTruthSet( String groundPath ) {
		ResultSet groundSet = new ResultSet(false);
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

	private void compareIntPairSetsWrapper( ResultSet groundSet, ResultSet rslt, Query query ) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(outputPath)));
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		compareIntPairSets(groundSet, rslt, query, pw);
		pw.close();
	}
	
	private void compareIntPairSets( ResultSet groundSet, ResultSet rslt, Query query, PrintWriter pw ) {
		/*	
		 * For the case of self joins, ipairs in the both sets are assumed to be orderd (i1 <= i2). 
		 *  if something is wrong, check it out.
		 */
		checkQuery(query);
		for ( final IntegerPair ipair : groundSet ) {
			if ( ipair.i1 == ipair.i2 ) continue;
			if ( rslt.contains(ipair) ) {
				if ( pw != null ) pw.println("TP"+(hasSameString(ipair)?"":"*")+"\t"+getRecordPairStringFromIntPair(ipair));
				++tp;
			}
			else {
				if ( pw != null ) pw.println("FN\t"+getRecordPairStringFromIntPair(ipair));
				++fn;
			}
		}
		for ( final IntegerPair ipair : rslt ) {
			if ( ipair.i1 == ipair.i2 ) continue;
			if ( canbeTransformed(ipair, query) ) continue;
			if ( !groundSet.contains(ipair) ) {
				if ( pw != null ) pw.println("FP\t"+getRecordPairStringFromIntPair(ipair));
				++fp;
			}
		}
	}
	
	private boolean hasSameString( IntegerPair ipair ) {
		return searchedSet.getRecord(ipair.i1).toString().equals( indexedSet.getRecord(ipair.i2).toString() );
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
	
	private void checkQuery( Query query ) {
		ACAutomataR automata = new ACAutomataR( query.ruleSet.get() );
		for( final Record record : query.searchedSet.get() ) {
			if ( record.getApplicableRules() == null ) record.preprocessApplicableRules( automata );
			if ( record.getSuffixApplicableRules(0) == null ) record.preprocessSuffixApplicableRules();
		}
	}
	
	private boolean canbeTransformed( Record s, Record t ) {
		return ( checker.isEqual(s, t) >= 0 );
	}
	
	private boolean canbeTransformed( IntegerPair ipair, Query query ) {
		Record s = query.searchedSet.getRecord(ipair.i1);
		Record t = query.indexedSet.getRecord(ipair.i2);
		return canbeTransformed(s, t);
	}
	
	private ResultSet filterOutTransEquivPairs( ResultSet rslt, Query query ) {
		ResultSet rslt_new = new ResultSet(query.selfJoin);
		for ( final IntegerPair ipair : rslt ) {
			if ( canbeTransformed(ipair, query) ) rslt_new.add(ipair);;
		}
		return rslt_new;
	}
}
