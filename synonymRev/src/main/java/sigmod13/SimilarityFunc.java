package sigmod13;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import snu.kdd.synonym.synonymRev.algorithm.SIJoinOriginal;
import snu.kdd.synonym.synonymRev.data.Rule;

public class SimilarityFunc {
	public static long invoked = 0;

	/**
	 * Calculate Full-expanded Jaccard similarity value
	 */
	public static double fullExp( SIRecord rec1, SIRecord rec2 ) {
		SIRecord erec1 = new SIRecord( rec1 );
		SIRecord erec2 = new SIRecord( rec2 );
		try {
			for( Rule rule : erec1.applicableRules )
				erec1.applyRule( rule );
			for( Rule rule : erec2.applicableRules )
				erec2.applyRule( rule );
		}
		catch( Exception e ) {
			// This should never be happen
			e.printStackTrace();
		}
		return erec1.calcJaccard( erec2 );
	}

	/**
	 * Calculate Full-expanded Jaccard similarity value <br/>
	 * Use their pre-expanded record
	 */
	public static double fullExp2( SIRecord rec1, SIRecord rec2 ) {
		return rec1.calcFullJaccard( rec2 );
	}

	public static double exactSelectiveExp( SIRecord rec1, SIRecord rec2 ) {
		double max = 0;
		HashSet<SIRecordExpanded> expanded = rec1.generateAll();
		for( SIRecordExpanded exp2 : rec2.generateAll() )
			for( SIRecordExpanded exp : expanded ) {
				double sim = exp2.jaccard( exp );
				max = Math.max( sim, max );
			}
		return max;
	}

	/**
	 * Calculate Selective-expanded Jaccard similarity value <br/>
	 * Algorithm 2 in the paper
	 */
	public static double selectiveExp( SIRecord rec1, SIRecord rec2, boolean expPrint ) {
		++invoked;
		if ( expPrint ) {
			if ( rec1.str.equals(rec2.str) ) expPrint = false;
			else SIJoinOriginal.pw.println( rec1.getID()+"\t"+rec1.str+"\n"+rec2.getID()+"\t"+rec2.str );
		}

		// Line 1 : Calcualte candidate rule set
		// Procedure findCandidateRuleSet(), line 4
		// Line 4 : Calcualte candidate rule set
		LinkedList<RuleGainPair> C1list = new LinkedList<RuleGainPair>();
		for( Rule rule : rec1.applicableRules ) {
			double gain = ruleGain( rule, rec1, rec2 );
			if( gain > 0 )
				C1list.add( new RuleGainPair( gain, rule ) );
		}
		LinkedList<RuleGainPair> C2list = new LinkedList<RuleGainPair>();
		for( Rule rule : rec2.applicableRules ) {
			double gain = ruleGain( rule, rec2, rec1 );
			if( gain > 0 )
				C2list.add( new RuleGainPair( gain, rule ) );
		}

		// Line 5 : repeat until no rule can be removed
		// TODO : Bottle neck
		boolean removed = false;
		do {
			removed = false;
			SIRecord erec1 = new SIRecord( rec1 );
			SIRecord erec2 = new SIRecord( rec2 );
			try {
				// Line 6 : Calculate S'_1
				for( RuleGainPair rgp : C1list )
					erec1.applyRule( rgp.rule );
				// Line 7 : Calculate S'_2
				for( RuleGainPair rgp : C2list )
					erec2.applyRule( rgp.rule );
			}
			catch( Exception e ) {
				// This should never be happen
				e.printStackTrace();
			}
			// Line 8 : calculate current Jaccard similarity
			double theta = erec1.calcJaccard( erec2 );
			double threshold = theta / ( 1 + theta );

			// Line 9 ~ 11
			Iterator<RuleGainPair> it1 = C1list.iterator();
			while( it1.hasNext() ) {
				RuleGainPair rgp = it1.next();
				if( rgp.rulegain < threshold ) {
					it1.remove();
					removed = true;
				}
			}

			Iterator<RuleGainPair> it2 = C2list.iterator();
			while( it2.hasNext() ) {
				RuleGainPair rgp = it2.next();
				if( rgp.rulegain < threshold ) {
					it2.remove();
					removed = true;
				}
			}
		}
		while( removed );

		// Line 2 : calculate \theta
		// Procedure expand(), line 13
		// Line 13 : Initialize S'_1
		SIRecord erec1 = new SIRecord( rec1 );
		// Line 14 : Initialize S'_2
		SIRecord erec2 = new SIRecord( rec2 );
		// Line 15 : repeat until there is no applicable rule
		HashSet<Rule> C1 = new HashSet<Rule>();
		HashSet<Rule> C2 = new HashSet<Rule>();
		for( RuleGainPair rgp : C1list )
			C1.add( rgp.rule );
		for( RuleGainPair rgp : C2list )
			C2.add( rgp.rule );
		while( C1.size() != 0 || C2.size() != 0 ) {
			// Line 16 : find the current most gain-effective rule
			Rule best_rule = null;
			double max_gain = Double.NEGATIVE_INFINITY;
			boolean best_from_1 = true;
			for( Rule rule : C1 ) {
				double gain = ruleGain( rule, erec1, erec2 );
				if( gain > max_gain ) {
					best_rule = rule;
					max_gain = gain;
				}
			}
			for( Rule rule : C2 ) {
				double gain = ruleGain( rule, erec2, erec1 );
				if( gain > max_gain ) {
					best_from_1 = false;
					best_rule = rule;
					max_gain = gain;
				}
			}

			// Line 17 : Check the best rule gain
			if( max_gain > 0 ) {
				// Line 18 : Expand
				try {
					if( best_from_1 ) {
						erec1.applyRule( best_rule );
						if ( expPrint ) SIJoinOriginal.pw.println("APPLY TO REC1: "+best_rule.toOriginalString(SIJoinOriginal.tokenMap));
					}
					else {
						erec2.applyRule( best_rule );
						if ( expPrint ) SIJoinOriginal.pw.println("APPLY TO REC2: "+best_rule.toOriginalString(SIJoinOriginal.tokenMap));
					}
				}
				catch( Exception e ) {
					// This should never be happen
					e.printStackTrace();
				}
			}
			else
				break;

			// Line 19 : remove the best rule
			if( best_from_1 )
				C1.remove( best_rule );
			else
				C2.remove( best_rule );
		}

		// Line 20 : return the similarity
		double sim = Math.max( erec1.calcJaccard( erec2 ), rec1.calcJaccard( rec2 ) );
		if ( expPrint ) {
			SIJoinOriginal.pw.println("SIM: "+sim);
			SIJoinOriginal.pw.flush();
		}
		return sim;
	}

	/**
	 * Calculate rule gain
	 *
	 * @param rule
	 *            An applicable rule of rec1
	 */
	private static double ruleGain( Rule rule, SIRecord rec1, SIRecord rec2 ) {
		// Line 1 : Calculate |U| instead of U
		int sizeU = 0;
		int sizeG = 0;
		for( int str : rule.getRight() ) {
			if( !rec1.contains( str ) ) {
				++sizeU;
				if( rec2.fullExpandedContains( str ) )
					++sizeG;
			}
		}
		if( sizeU == 0 )
			return 0;
		return (double) sizeG / sizeU;
	}
}

class RuleGainPair {
	double rulegain;
	Rule rule;

	RuleGainPair( double rulegain, Rule rule ) {
		this.rulegain = rulegain;
		this.rule = rule;
	}

	@Override
	public int hashCode() {
		return rule.hashCode();
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}

		RuleGainPair rgp = (RuleGainPair) o;
		return rule.equals( rgp.rule );
	}
}
