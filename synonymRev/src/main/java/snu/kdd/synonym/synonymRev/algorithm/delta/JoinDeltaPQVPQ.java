package snu.kdd.synonym.synonymRev.algorithm.delta;

public class JoinDeltaPQVPQ extends JoinDeltaVarBK {
	
	public JoinDeltaPQVPQ(String[] args) {
		super(args);
	}

	@Override
	protected void buildIndex() {
		JoinDeltaPQVPQIndex.useLF = useLF;
		JoinDeltaPQVPQIndex.usePQF = usePQF;
		JoinDeltaPQVPQIndex.useSTPQ = useSTPQ;
		idx = new JoinDeltaPQVPQIndex(query, indexK, qSize, deltaMax, distFunc, sampleB);
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: the initial version
		 * 1.01: apply filter in JoinDeltaVar first
		 */
		return "1.01";
	}

	@Override
	public String getName() {
		return "JoinDeltaPQVPQ";
	}
}
