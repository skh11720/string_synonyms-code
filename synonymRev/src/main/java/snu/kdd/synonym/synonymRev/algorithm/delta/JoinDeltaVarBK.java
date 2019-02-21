package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.data.Query;

public class JoinDeltaVarBK extends JoinDeltaVar {
	
	protected final double sampleB;

	public JoinDeltaVarBK(Query query, String[] args) {
		super(query, args);
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
	public String getOutputName() {
		return String.format( "%s_d%d", super.getOutputName(), deltaMax );
	}
}
