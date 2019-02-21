package snu.kdd.synonym.synonymRev.algorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.AbstractParam;
import snu.kdd.synonym.synonymRev.tools.AlgorithmResultQualityEvaluator;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public abstract class AbstractAlgorithm implements AlgorithmInterface, AlgorithmStatInterface {

	protected final Query query;
	protected final StatContainer stat;
	protected Validator checker = null;
	protected AbstractParam param;
	public Set<IntegerPair> rslt = null;
	public boolean writeResult = true;


	public AbstractAlgorithm( Query query, String[] args ) {
		this.stat = new StatContainer();
		this.query = query;
		stat.add( "alg", getName() );
		stat.add( "alg_version", getVersion() );
	}

	public abstract String getName();

	public abstract String getVersion();
	
	protected abstract void executeJoin();

	public void run() {
		StopWatch totalTime = StopWatch.getWatchStarted(TOTAL_RUNNING_TIME);
		preprocess();
		executeJoinWrapper();
		totalTime.stop();
		stat.addPrimary(totalTime);

		if (checker != null) checker.addStat(stat);
		writeResult();
		Util.printGCStats( stat, "Stat" );
		stat.resultWriter( "result/" + getName() + "_" + getVersion() );
	}

	protected void preprocess() {
		StopWatch watch = StopWatch.getWatchStarted(PREPROCESS_TOTAL_TIME);
		preprocessRules();
		computeTransformLengths();
		estimateNumTransforms();
		watch.stopQuietAndAdd(stat);
		stat.addMemory("Mem_2_Preprocessed");
	}
	
	private void preprocessRules() {
		StopWatch watch = StopWatch.getWatchStarted(PREPROCESS_RULE_TIME);
		ACAutomataR automata = new ACAutomataR( query.ruleSet.get() );
		long applicableRules = 0;
		for( final Record record : query.searchedSet.get() ) {
			record.preprocessApplicableRules( automata );
			record.preprocessSuffixApplicableRules();
			applicableRules += record.getNumApplicableRules();
		}
		watch.stopQuietAndAdd(stat);
		stat.add( "Stat_Applicable_Rule_TableSearched", applicableRules );
		stat.add( "Stat_Avg_applicable_rules", Double.toString( (double) applicableRules / query.searchedSet.size() ) );
	}
	
	private final void computeTransformLengths() {
		StopWatch watch = StopWatch.getWatchStarted(PREPROCESS_LENGTH_TIME);
		for( final Record rec : query.searchedSet.get() ) {
			rec.preprocessTransformLength();
		}
		watch.stopQuietAndAdd(stat);
	}
	
	private final void estimateNumTransforms() {
		StopWatch watch = StopWatch.getWatchStarted(PREPROCESS_EST_NUM_TRANS_TIME);
		long maxNumTrans = 0;
		for( final Record rec : query.searchedSet.get() ) {
			rec.preprocessEstimatedRecords();
			long est = rec.getEstNumTransformed();
			if( maxNumTrans < est ) {
				maxNumTrans = est;
			}
		}
		watch.stopQuietAndAdd(stat);
		stat.add( "Stat_maximum_Size_of_TableSearched", maxNumTrans );
	}

	private void executeJoinWrapper() {
		StopWatch watch = StopWatch.getWatchStarted(JOIN_TOTAL_TIME);
		executeJoin();
		watch.stopAndAdd( stat );
	}

	public void writeResult() {
		if ( !writeResult ) return;
		stat.addPrimary( "Final_Result_Size", rslt.size() );

		try {
			if( DEBUG.AlgorithmON ) {
				Util.printLog( "Writing results " + rslt.size() );
			}

			BufferedWriter bw = new BufferedWriter( new FileWriter( String.format( "%s/%s_output.txt", query.outputPath, getOutputName() ) ) );

			bw.write( rslt.size() + "\n" );
			for( final IntegerPair ip : rslt ) {
				final Record r = query.searchedSet.getRecord( ip.i1 );
				final Record s = query.indexedSet.getRecord( ip.i2 );

				if( !DEBUG.printSelfJoinON ) {
					if( query.selfJoin && r.equals( s ) ) {
						continue;
					}
				}

//				bw.write( r.toString( query.tokenIndex ) + "(" + r.getID() + ")\t==\t" + s.toString( query.tokenIndex ) + "("+ s.getID() + ")\n" );
//				bw.write( "(" + r.getID() + ")\t==\t" + "("+ s.getID() + ")\n" );
//				bw.write( Arrays.toString( r.getTokensArray() ) + "(" + r.getID() + ")\t==\t" + Arrays.toString( s.getTokensArray() ) + "("+ s.getID() + ")\n" );
				bw.write( r.toString() + "(" + r.getID() + ")\t==\t" + s.toString() + "("+ s.getID() + ")\n" );
			}
			bw.close();
		}
		catch( final Exception e ) {
			e.printStackTrace();
			Util.printLog( "Error: " + e.getMessage() );
		}
	}

	@Override
	public void writeJSON() {
		BufferedWriter bw_json;
		try {
			bw_json = new BufferedWriter( new FileWriter(
					"json/" + this.getName() + "_"
							+ new java.text.SimpleDateFormat( "yyyyMMdd_HHmmss_z" ).format( new java.util.Date() ) + ".txt",
					true ) );

			// start JSON object
			bw_json.write( "{" );
			// metadata
			bw_json.write( "\"Date\":\"" + new Date().toString()+"\"");

			// dataset
			bw_json.write( ", \"Dataset\":{" );
			bw_json.write( query.dataInfo.toJson() );
			bw_json.write( "}" );

			// algorithm
			bw_json.write( ", \"Algorithm\":{" );
				bw_json.write( "\"Name\":\"" + getName() + "\", " );
				bw_json.write( "\"Version\":\"" + getVersion()+"\"");
			bw_json.write( "}" );
			
			// param
			if ( param == null ) bw_json.write(", \"Param\": \"\"");
			else bw_json.write(", \"Param\":"+param.getJSONString() );

			// output
			bw_json.write( ", \"Output\":{" );
			bw_json.write( stat.toJson() );
			bw_json.write( "}" );

			bw_json.write( "}\n" );
			bw_json.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	@Override
	public Set<IntegerPair> getResult() {
		return rslt;
	}
	
	public static void addSeqResult( Record rec1, Record rec2, Set<IntegerPair> rslt, boolean isSelfJoin ) {
		if ( isSelfJoin ) {
			int id_smaller = rec1.getID() < rec2.getID()? rec1.getID() : rec2.getID();
			int id_larger = rec1.getID() >= rec2.getID()? rec1.getID() : rec2.getID();
			rslt.add( new IntegerPair( id_smaller, id_larger) );
		}
		else rslt.add( new IntegerPair(rec1.getID(), rec2.getID()) );
	}

	public static void addSeqResult( Record rec1, int rec2id, Set<IntegerPair> rslt, boolean isSelfJoin ) {
		if ( isSelfJoin ) {
			int id_smaller = rec1.getID() < rec2id? rec1.getID() : rec2id;
			int id_larger = rec1.getID() >= rec2id? rec1.getID() : rec2id;
			rslt.add( new IntegerPair( id_smaller, id_larger) );
		}
		else rslt.add( new IntegerPair(rec1.getID(), rec2id) );
	}
	
	public static void addSetResult( Record rec1, Record rec2, Set<IntegerPair> rslt, boolean leftFromS, boolean isSelfJoin ) {
		if ( isSelfJoin ) {
			int id_smaller = rec1.getID() < rec2.getID()? rec1.getID() : rec2.getID();
			int id_larger = rec1.getID() >= rec2.getID()? rec1.getID() : rec2.getID();
			rslt.add( new IntegerPair( id_smaller, id_larger) );
		}
		else {
			// idx == idxT
			if ( leftFromS ) rslt.add( new IntegerPair( rec1.getID(), rec2.getID()) );
			// idx == idxS
			else rslt.add( new IntegerPair( rec2.getID(), rec1.getID()) );
		}
	}
	
	public void getEvaluationResult( AlgorithmResultQualityEvaluator eval ) {
		stat.add(EVAL_TP, eval.tp);
		stat.add(EVAL_FP, eval.fp);
		stat.add(EVAL_FN, eval.fn);
		stat.add(EVAL_PRECISION, eval.getPrecision());
		stat.add(EVAL_RECALL, eval.getRecall());
	}
	
	@Override
	public void setWriteResult( boolean flag ) {
		this.writeResult = flag;
	}
	
	@Override
	public StatContainer getStat() { return stat; }
	

	public String getOutputName() {
		String[] tokens = query.getSearchedPath().split("\\"+File.separator);
		String data1Name = tokens[tokens.length-1].split("\\.")[0];
		String data2Name = null;
		if ( !query.selfJoin ) {
			tokens = query.getIndexedPath().split("\\"+File.separator);
			data2Name = tokens[tokens.length-1].split("\\.")[0];
		}
		if ( query.selfJoin ) return getName()+"_"+data1Name;
		else return getName()+"_"+data1Name+"_"+data2Name;
	}
}
