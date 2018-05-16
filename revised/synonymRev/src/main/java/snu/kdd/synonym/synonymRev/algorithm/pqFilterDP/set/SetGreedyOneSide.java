package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

public class SetGreedyOneSide extends AbstractSetValidator {
	
	private final int beamWidth; 
	private Beam[] beamList;
	/*
	 * When beamWitdh = 3, beams are expanded as follows.
	 * Beginning of a loop: beam0, beam1, beam2 (decreasing order of scores)
	 * After expansion: beam0_0, beam1_0, beam2_0, beam0_1, beam1_1, beam2_1, beam0_2, beam1_2, beam2_2
	 * End of a loop: beam0', beam1', beam2'
	 */
	
	public SetGreedyOneSide( Boolean selfJoin, int beamWidth ) {
		super( selfJoin	 );
		beamList = new Beam[beamWidth*beamWidth];
		for ( int k=0; k<beamList.length; k++ ) beamList[k] = new Beam();
		this.beamWidth = beamWidth;

	}

	@Override
	protected int isEqualOneSide( Record x, Record y ) {
		// check whether x -> y
		
		// DEBUG
//		System.out.println( x.getID()+", "+y.getID() );
		Boolean debug = false;
//		if ( x.getID() == 7858 && y.getID() == 7914 ) debug = true;
//		if ( y.getID() == 7858 && x.getID() == 7914 ) debug = true;
		if (debug) {
			System.out.println( x.toString()+", "+Arrays.toString( x.getTokensArray() ) );
			System.out.println( y.toString()+", "+Arrays.toString( y.getTokensArray() ) );
		}
		
		IntOpenHashSet ySet = new IntOpenHashSet(y.getTokensArray());
		int pos = x.size();
		beamList[0].set( 0, pos, ySet );
		for ( int k=1; k<beamList.length; k++) beamList[k].clear();
		
//		boolean isEqual = getMyEqual( , x.size() );
		
		if (debug) System.out.println( "beamList: "+Arrays.toString( beamList ) );
		
		Boolean finished = false;
//		for ( int count=0; count<max_iter && !finished; count++ ) {
		while (!finished) {
			finished = true;
			for ( int k=0; k<beamWidth; k++) {
				Beam beam = beamList[k];
				if ( beam.remaining == null ) break;
				if ( beam.pos == 0 ) continue;
				IntOpenHashSet remaining = new IntOpenHashSet(beam.remaining);
				int beamScore = beam.score;
				int beamPos = beam.pos;

				finished = false;
				Rule[] rules = x.getSuffixApplicableRules( beamPos-1 );
				ScoreRule[] scoreRules = new ScoreRule[rules.length];
				for ( int i=0; i<rules.length; i++ ) {
					Rule rule = rules[i];
					if (debug) System.out.println( beamPos+", "+rule );

					++niterrules;
					// check whether all tokens in rhs are in y.
					int score = 0;

					Boolean isValidRule = true;
					for ( int token : rule.getRight() ) {
						if ( !ySet.contains( token ) ) {
							isValidRule = false;
							break;
						}
						if ( remaining.contains( token ) ) ++score;
					}
					if ( isValidRule ) {
						scoreRules[i] = new ScoreRule( rule, score );
//						if (debug) System.out.println( scoreRules[i] );
					}
					else scoreRules[i] = new ScoreRule();
				} // end for rules
				
				// If there is no valid rule, return -1.
//				if (debug) System.out.println( Arrays.toString( scoreRules ) );
				Arrays.sort( scoreRules );
				if (debug) System.out.println( "scoreRules: "+Arrays.toString( scoreRules ) );
				
				// DEBUG
	//			if ( debug ) System.out.println( "best: "+bestRule.toString() );
				for ( int l=0; l<beamWidth; l++ ) {
					if ( l >= rules.length || scoreRules[l].rule == null )
						beamList[k+l*beamWidth].clear();
					else {
						Rule rule = scoreRules[l].rule;
						int score = scoreRules[l].score;
						IntOpenHashSet newRemaining = new IntOpenHashSet(remaining);
						for ( int token : rule.getRight() ) newRemaining.remove( token );
						beamList[k+l*beamWidth].set(beamScore+score, beamPos - rule.leftSize(), newRemaining);
					}
				}
			} // end for beamList

			// Compress the beamList.
			if (debug) System.out.println( "beamList, before comp: "+Arrays.toString( beamList ) );
			Arrays.sort( beamList );
			for ( int k=beamWidth; k<beamWidth*beamWidth; k++) {
				if ( beamList[k].remaining == null ) break;
				else beamList[k].clear();
			}
			if (debug) System.out.println( "beamList, after comp: "+Arrays.toString( beamList ) );
				
		} // end while
		
//		if ( debug ) System.out.println( "result: "+(remaining.size() == 0) );
		
		if ( beamList[0].remaining == null ) return -1;
		if ( beamList[0].remaining.size() == 0 ) return 1;
		else return -1;
	}

	@Override
	public String getName() {
		return "SetGreedyOneSide";
	}
	
	private class Beam implements Comparable<Beam> {
		int score;
		int pos;
		IntOpenHashSet remaining;
		
		public Beam() {
			clear();
		}
		
		@Override
		public boolean equals( Object obj ) {
			if (obj == null) return false;
			Beam o = (Beam)obj;
			return (score == o.score && pos == o.pos && remaining.equals( o.remaining ));
		}

		@Override
		public int compareTo( Beam o ) {
			/*
			 * NOTE: sort by decreasing order of scores
			 */
			if ( o == null ) return -1;
			if ( score < o.score ) return 1;
			else if ( score > o.score ) return -1;
			else return 0;
		}
		
		@Override
		public String toString() {
			if (remaining == null) return "("+score+", "+pos+", null)";
			else return "("+score+", "+pos+", "+remaining.toString()+")";
		}
		
		public void set( int score, int pos, IntOpenHashSet remaining ) {
			this.score = score;
			this.pos = pos;
			this.remaining = remaining;
		}
		
		public void clear() {
			this.score = -1;
			this.pos = 0;
			this.remaining = null;
		}
	}
	
	private class ScoreRule implements Comparable<ScoreRule> {
		Rule rule;
		int score;
		
		public ScoreRule() {
			clear();
		}
		
		public ScoreRule( Rule rule, int score ) {
			set(rule, score);
		}
		
		@Override
		public boolean equals( Object obj ) {
			if ( obj == null ) 	return false;
			ScoreRule o = (ScoreRule)obj;
			return (rule.equals( o.rule ) && score == o.score );
		}
		
		@Override
		public int compareTo( ScoreRule o ) {
			/*
			 * NOTE: sort by decreasing order of scores
			 */
			if ( o == null ) return -1;
			if ( score < o.score ) return 1;
			else if ( score > o.score ) return -1;
			else return 0;
		}
		
		@Override
		public String toString() {
			if ( rule == null ) return "(null, "+score+")";
			else return "("+rule.toString()+", "	+score+")";
		}
		
		public void set( Rule rule, int score ) {
			this.rule = rule;
			this.score = score;
		}
		
		public void clear() {
			this.rule = null;
			this.score = -1;
		}
	}
}
