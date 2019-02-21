
package snu.kdd.synonym.synonymRev.tools;

import snu.kdd.synonym.synonymRev.algorithm.AbstractAlgorithm;
import snu.kdd.synonym.synonymRev.algorithm.set.ParamForSet;
import vldb17.ParamPkduck;

public class ParamFactory {

	public static AbstractParam getParamInstance( AbstractAlgorithm alg, String[] args ) {
		if ( alg.getName().equals("JoinPkduck") ||
		 	 alg.getName().equals("JoinPkduckSet") ||
			 alg.getName().equals("JoinPkduckOriginal") ) {
			return new ParamPkduck(args);
		}
		if ( alg.getName().equals("JoinBKPSet") )
			return new ParamForSet(args);
		else 
			return new Param(args);
	}
}
