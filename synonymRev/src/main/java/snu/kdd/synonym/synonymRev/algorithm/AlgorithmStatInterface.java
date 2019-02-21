package snu.kdd.synonym.synonymRev.algorithm;

public interface AlgorithmStatInterface {
	public static final String TOTAL_RUNNING_TIME = "Time_0_Total";
	public static final String PREPROCESS_TOTAL_TIME = "Time_1_Preprocess_Total";
	public static final String PREPROCESS_RULE_TIME = "Time_1_1_Preprocess_Rule";
	public static final String PREPROCESS_LENGTH_TIME = "Time_1_2_Preprocess_Length";
	public static final String PREPROCESS_EST_NUM_TRANS_TIME = "Time_1_3_Preprocess_Est_Num_Trans";
	public static final String JOIN_TOTAL_TIME = "Time_2_Join_Total";
	public static final String INDEX_BUILD_TIME = "Time_2_1_Index_Building";
	public static final String JOIN_AFTER_INDEX_TIME = "Time_2_2_Join_After_Index";
//	public static final String FILTER_TIME = "Time_2_2_Filter";
//	public static final String VERIFY_TIME = "Time_2_3_Verify";
	
	public static final String FINAL_RESULT_SIZE = "Final_Result_Size";

	public static final String EVAL_TP = "Eval_TP";
	public static final String EVAL_FP = "Eval_FP";
	public static final String EVAL_FN = "Eval_FN";
	public static final String EVAL_PRECISION = "Eval_Precision";
	public static final String EVAL_RECALL = "Eval_Recall";
}
