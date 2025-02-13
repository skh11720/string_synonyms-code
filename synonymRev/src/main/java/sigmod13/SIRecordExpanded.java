package sigmod13;

import java.util.Collection;

import javax.management.RuntimeErrorException;

import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntegerSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

/**
 * Expanded record
 */
public class SIRecordExpanded implements SIRecord.Expanded {
	private final SIRecord rec;
	private final Collection<Integer> originalTokens;
	private final IntegerSet expandedTokens;

	public SIRecordExpanded( SIRecord rec ) {
		this.rec = rec;
		originalTokens = rec.getTokens();
		expandedTokens = new IntegerSet();
	}

	public SIRecordExpanded( SIRecordExpanded rec_exp ) {
		this.rec = rec_exp.rec;
		originalTokens = new IntegerSet( rec_exp.originalTokens );
		expandedTokens = new IntegerSet( rec_exp.expandedTokens );
	}

	@Override
	public SIRecord toRecord() {
		return rec;
	}

	public Collection<Integer> getOriginalTokens() {
		return originalTokens;
	}

	public IntegerSet getExpandedTokens() {
		return expandedTokens;
	}

	public void applyRule( Rule rule ) {
		for( int s : rule.getLeft() ) {
			if( !originalTokens.contains( s ) ) {
				throw new RuntimeException( "Not applicable rule" );
			}
		}
		for( int s : rule.getRight() ) {
			if( !originalTokens.contains( s ) ) {
				expandedTokens.add( s );
			}
		}
	}

	@Override
	public String toString() {
		return originalTokens.toString() + " + " + expandedTokens.toString();
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}
		SIRecordExpanded sire = (SIRecordExpanded) o;
		if( hashCode() != sire.hashCode() )
			return false;
		return originalTokens.equals( sire.originalTokens ) && expandedTokens.equals( sire.expandedTokens );
	}

	@Override
	public int hashCode() {
		return originalTokens.hashCode() + expandedTokens.hashCode();
	}

	public double jaccard( SIRecordExpanded o ) {
		int intersection = 0;
		for( Integer token : originalTokens ) {
			if( o.originalTokens.contains( token ) )
				++intersection;
			else if( o.expandedTokens.contains( token ) )
				++intersection;
		}
		for( Integer token : expandedTokens ) {
			if( o.originalTokens.contains( token ) )
				++intersection;
			else if( o.expandedTokens.contains( token ) )
				++intersection;
		}
		int union = originalTokens.size() + expandedTokens.size() + o.originalTokens.size() + o.expandedTokens.size()
				- intersection;
		return (double) intersection / union;
	}

	@Override
	public int size() {
		return originalTokens.size() + expandedTokens.size();
	}

	@Override
	public double similarity( RecordInterface.Expanded o, Validator checker ) {
		if( o.getClass() != SIRecordExpanded.class )
			return 0;
		return jaccard( (SIRecordExpanded) o );
	}
}
