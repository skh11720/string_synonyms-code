package sigmod13;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import sigmod13.filter.ITF_Filter;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.data.TokenIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerSet;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SIRecord implements RecordInterface, Comparable<SIRecord> {
	private final int id;
	private final int[] tokens;
	private final IntegerSet tokenSet;
	final IntegerSet fullExpanded;
	final ObjectArrayList<Rule> applicableRules;
	final ObjectArrayList<Rule> applicableNonSelfRules;
	
	public String str;

	/**
	 * Create a record and preprocess applicable rules
	 */
	public SIRecord( int id, String str, TokenIndex tokenIndex ) {
		this.id = id;
		this.str = str;
		String[] pstr = str.split( "( |\t)+" );
		this.tokens = new int[ pstr.length ];
		this.tokenSet = new IntegerSet();
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = tokenIndex.getID( pstr[ i ] );
			this.tokenSet.add( tokens[ i ] );
		}

		applicableRules = new ObjectArrayList<Rule>();
		applicableNonSelfRules = new ObjectArrayList<Rule>();
		fullExpanded = this.tokenSet.copy();
	}

	public SIRecord( SIRecord rec ) {
		id = -1;
		tokens = Arrays.copyOf(rec.tokens, rec.tokens.length);
		tokenSet = rec.tokenSet.copy();
		// Applicable rules does not change
		applicableRules = rec.applicableRules;
		applicableNonSelfRules = getNonSelfRules(applicableRules);
		fullExpanded = rec.fullExpanded;
	}

	public SIRecord( SIRecordExpanded rec ) {
		this(rec.toRecord());
	}

	@Override
	public int getID() {
		return id;
	}
	
	public ObjectArrayList<Rule> getNonSelfRules( ObjectArrayList<Rule> applicableRules ) {
		ObjectArrayList<Rule> output = new ObjectArrayList<>();
		for ( Rule rule : applicableRules ) {
			if ( !rule.isSelfRule() ) output.add(rule);
		}
		return output;
	}
	
	public void preprocess( ACAutomataR automata ) {
		// Rules
		for( Rule rule : automata.applicableRulesSIRecord( tokens ) ) {
			applicableRules.add( rule );
			if ( !rule.isSelfRule() ) applicableNonSelfRules.add(rule);
		}
		
		// Full expand
		for( Rule rule : applicableRules )
			for( int s : rule.getRight() )
				fullExpanded.add( s );
	}

	public final ObjectArrayList<Rule> getApplicableRules() {
		return applicableRules;
	}

	/**
	 * Generate all the possible expanded sets
	 */
	@Override
	public HashSet<SIRecordExpanded> generateAll() {
		try {
			Queue<SIRecordExpanded> queue = new LinkedList<SIRecordExpanded>();
			queue.add( new SIRecordExpanded( this ) );

			Queue<SIRecordExpanded> bufferQueue = new LinkedList<SIRecordExpanded>();
			for( Rule rule : applicableRules ) {
				if( rule.getLeft().length == 1 && rule.getRight().length == 1 && rule.getLeft()[ 0 ] == rule.getRight()[ 0 ] )
					continue;
				while( !queue.isEmpty() ) {
					SIRecordExpanded curr = queue.poll();
					SIRecordExpanded expanded = new SIRecordExpanded( curr );
					expanded.applyRule( rule );
					bufferQueue.add( curr );
					bufferQueue.add( expanded );
				}
				Queue<SIRecordExpanded> tmpqueue = bufferQueue;
				bufferQueue = queue;
				queue = tmpqueue;
			}
			HashSet<SIRecordExpanded> rslt = new HashSet<SIRecordExpanded>( queue );
			return rslt;
		}
		catch( Exception e ) {
			e.printStackTrace();
			return null;
		}
	}

	public void applyRule( Rule rule ) throws Exception {
		for( int s : rule.getLeft() )
			if( !tokenSet.contains( s ) )
				throw new Exception( "Not applicable rule" );
		for( int s : rule.getRight() )
			tokenSet.add( s );
	}

	public void applyRule( HashSet<Rule> rules ) throws Exception {
		for( Rule rule : rules ) {
			for( int s : rule.getLeft() )
				if( !tokenSet.contains( s ) )
					throw new Exception( "Not applicable rule" );
			for( int s : rule.getRight() )
				tokenSet.add( s );
		}
	}

	public double calcJaccard( SIRecord o ) {
		int cupsize = 0;
		for( Integer str : tokenSet )
			if( o.tokenSet.contains( str ) )
				++cupsize;
		return (double) cupsize / (double) ( tokenSet.size() + o.tokenSet.size() - cupsize );
	}

	public double calcFullJaccard( SIRecord o ) {
		int cupsize = 0;
		for( Integer str : fullExpanded )
			if( o.fullExpanded.contains( str ) )
				++cupsize;
		return (double) cupsize / (double) ( fullExpanded.size() + o.fullExpanded.size() - cupsize );
	}

	public boolean contains( int token ) {
		return tokenSet.contains( token );
	}

	public boolean fullExpandedContains( int token ) {
		return fullExpanded.contains( token );
	}

	@Override
	public int hashCode() {
		return tokenSet.hashCode();
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}
		return id == ( (SIRecord) o ).id;
	}

	@Override
	public String toString() {
		return tokenSet.toString();
	}

	@Override
	public int getMinLength() {
		return tokenSet.size();
	}

	@Override
	public int getMaxLength() {
		return fullExpanded.size();
	}

	@Override
	public int size() {
		return tokenSet.size();
	}

	@Override
	public Collection<Integer> getTokens() {
		return tokenSet;
	}

	@Override
	public Set<Integer> getSignatures( ITF_Filter filter, double theta ) {
		IntOpenHashSet signature = new IntOpenHashSet();
		// 19.01.09. commented out: why this fi block is used?????
		// this block does not generate the correct signature since it does not expand the record.
//		if( theta == 1 ) { 
//			signature.addAll( filter.filter( new SIRecordExpanded( this ), 1 ) );
//			return signature;
//		}
//		HashSet<SIRecordExpanded> expanded = generateAll();
		Iterator<SIRecordExpanded> iter = getExpandIterator();
//		if ( expanded == null ) return null;
		while ( iter.hasNext() ) {
			SIRecordExpanded exp = iter.next();
			// In the paper the number of signature is states as belows.
			// int cut = (int) Math.ceil((1.0 - theta) * exp.size());
			// However, it should be
			int cut = 1 + exp.size() - (int) Math.ceil( theta * exp.size() );
			HashSet<Integer> sig = filter.filter( exp, cut );
			signature.addAll( sig );
		}
		return signature;
	}

	@Override
	// use selectiveExp
	public double similarity( RecordInterface rec, Validator checker ) {
		if( rec.getClass() != SIRecord.class )
			return 0;
		return SimilarityFunc.selectiveExp( this, (SIRecord) rec, false );
	}

	@Override
	public int compareTo( SIRecord o ) {
		return Integer.compare( id, o.id );
	}
	
	public Iterator<SIRecordExpanded> getExpandIterator() {
		return new ExpandIterator();
	}
	
	class ExpandIterator implements Iterator<SIRecordExpanded> {
		
		private int rptr = -1;
		private final ObjectArrayList<Rule> ruleList = SIRecord.this.applicableNonSelfRules;
		private final int nRule = ruleList.size();
		private SIRecordExpanded[] stackExp = new SIRecordExpanded[nRule+1];
		private int[] stack = new int[nRule+1];
		private int sptr = 0;
		private boolean hasNext = true;
		
		public ExpandIterator() {
			stackExp[0] = new SIRecordExpanded(SIRecord.this);
			stack[0] = -1;
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public SIRecordExpanded next() {
			SIRecordExpanded exp = stackExp[sptr];
			if ( rptr == nRule-1 ) {
				if ( --sptr <= 0 ) hasNext = false;
				else {
					rptr = ++stack[sptr];
					stackExp[sptr] = new SIRecordExpanded(stackExp[sptr-1]);
					stackExp[sptr].applyRule(ruleList.get(rptr));
				}
			}
			else {
				stack[++sptr] = ++rptr;
				stackExp[sptr] = new SIRecordExpanded(stackExp[sptr-1]);
				stackExp[sptr].applyRule(ruleList.get(rptr));
			}
			return exp;
		}
	}
	
	public static void enumerateExample() {
//	public static void main( String[] args ) {
		int n = 4;
		int i = -1;
		int[] stack = new int[n+1];
		Arrays.fill(stack, -1);
		int depth = 0;
		
		while (true) {
			System.out.println(Arrays.toString(ArrayUtils.subarray(stack, 0, depth+1)));
			if ( i == n-1 ) { // have the last element
				if ( --depth == 0 ) break;
				else i = ++stack[depth];
			}
			else stack[++depth] = ++i;
		}
	}
	
//	public static void main( String[] args ) throws IOException {
	public static void enumerateExample2() throws IOException {
		Query query = Util.getTestQuery( "AOL", 10000 );
		ACAutomataR automata = new ACAutomataR(query.ruleSet.get());
		Record.tokenIndex = query.tokenIndex;
		for ( int i=0; i<query.searchedSet.size(); ++i 	) {
			Record rec = query.searchedSet.getRecord(i);
			SIRecord sirec = new SIRecord( rec.getID(), rec.toString(), Record.tokenIndex);
			sirec.preprocess(automata);
			if ( sirec.applicableRules.size() == 6) {
				System.out.println(sirec.id);
				System.out.println(sirec);
				System.out.println(sirec.applicableNonSelfRules);

				Iterator<SIRecordExpanded> iter = sirec.getExpandIterator();
				while (iter.hasNext()) {
					System.out.println(iter.next());
				}


				break;
			}
		}
	}
}
