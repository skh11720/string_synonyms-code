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
		idx = new JoinDeltaVarBKIndex(query, indexK, qSize, deltaMax, sampleB);
		idx.build();
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: the initial version
		 */
		return "1.00";
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
