package snu.kdd.synonym.synonymRev.tools;

public class IntTriple {
	int n1, n2, n3;
	private final int hash;
	
	public IntTriple(int n1, int n2, int n3) {
		this.n1 = n1;
		this.n2 = n2;
		this.n3 = n3;
		int hash = 0;
		hash = 0x1f1f1f1f ^ hash + n1;
		hash = 0x1f1f1f1f ^ hash + n2;
		hash = 0x1f1f1f1f ^ hash + n3;
		this.hash = hash;
	}
	
	@Override
	public int hashCode() {
		return this.hash;
	}
	
	@Override
	public boolean equals( Object obj ) {
		IntTriple o = (IntTriple)obj;
		if ( this.n1 == o.n1 && this.n2 == o.n2 && this.n3 == o.n3 ) return true;
		else return false;
	}
}