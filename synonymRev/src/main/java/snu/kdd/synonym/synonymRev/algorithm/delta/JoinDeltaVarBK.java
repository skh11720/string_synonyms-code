package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaVarBK extends JoinDeltaVar {
	
	protected final double sampleB;

	public JoinDeltaVarBK(Query query, StatContainer stat, String[] args) throws IOException, ParseException {
		super(query, stat, args);
		sampleB = param.getDoubleParam("sampleB");
		stat.add("sampleB", sampleB);
	}

	protected void buildIndex( boolean writeResult ) {
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
		return String.format( "%s_d%d", getName(), deltaMax );
	}
}
