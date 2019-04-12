package snu.kdd.synonym.synonymRev.algorithm.delta;

public class JoinDeltaVarBK extends JoinDeltaVar {
	
	protected final double sampleB;

	public JoinDeltaVarBK(String[] args) {
		super(args);
		sampleB = param.getDoubleParam("sampleB");
	}

	@Override
	protected void reportParamsToStat() {
		super.reportParamsToStat();
		stat.add("Param_sampleB", sampleB);
	}

	@Override
	protected void buildIndex() {
		JoinDeltaVarBKIndex.useLF = useLF;
		JoinDeltaVarBKIndex.usePQF = usePQF;
		JoinDeltaVarBKIndex.useSTPQ = useSTPQ;
		idx = JoinDeltaVarBKIndex.getInstance(query, indexK, qSize, deltaMax, distFunc, sampleB);
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: the initial version
		 * 1.01: major update
		 * 1.02: fix bug in delta-q-gram generation
		 */
		return "1.02";
	}

	@Override
	public String getName() {
		return "JoinDeltaVarBK";
	}
	
	@Override
	public String getNameWithParam() {
		return String.format("%s_%d_%d_%d_%s_%.2f", getName(), indexK, qSize, deltaMax, distFunc, sampleB);
	}
}
