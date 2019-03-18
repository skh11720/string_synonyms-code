package snu.kdd.synonym.synonymRev.tools;

import java.util.Iterator;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;

public class ResultSet implements Iterable<IntegerPair> {

	private final boolean isSelfJoin;
	private final Set<IntegerPair> pairSet;
	
	public ResultSet( boolean isSelfJoin ) {
		this.isSelfJoin = isSelfJoin;
		pairSet = new ObjectOpenHashSet<>();
	}
	
	public boolean contains( IntegerPair ipair ) {
		if (isSelfJoin) {
			return pairSet.contains(ipair) || pairSet.contains(ipair.swap());
		}
		else {
			return pairSet.contains(ipair);
		}
	}
	
	public boolean contains( Record rec1, Record rec2 ) {
		return this.contains( new IntegerPair(rec1.getID(), rec2.getID()) );
	}
	
	public void add( IntegerPair ipair ) {
		if ( this.contains(ipair) ) return;
		if (isSelfJoin) pairSet.add(ipair.ordered());
		else pairSet.add(ipair);
	}
	
	public void add( Record rec1, Record rec2 ) {
		this.add( new IntegerPair( rec1.getID(), rec2.getID() ) );
	}
	
	public void add( Record rec1, int rec2id ) {
		this.add( new IntegerPair( rec1.getID(), rec2id ) );
	}
	
	public void addAll( ResultSet otherSet ) {
		for ( IntegerPair ipair : otherSet ) {
			this.add(ipair);
		}
	}
	
	public void addAll( Set<IntegerPair> otherSet ) {
		for ( IntegerPair ipair : otherSet ) {
			this.add(ipair);
		}
	}
	
	public int size() {
		return pairSet.size();
	}

	@Override
	public Iterator<IntegerPair> iterator() {
		return pairSet.iterator();
	}
}
