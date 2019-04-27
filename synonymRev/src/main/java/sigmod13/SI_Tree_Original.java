package sigmod13;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sigmod13.filter.ITF_Filter;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerMap;
import snu.kdd.synonym.synonymRev.tools.Pair;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SI_Tree_Original<T extends RecordInterface & Comparable<T>> {

	public static boolean skipEquiCheck = false;
	/**
	 * A root entry of S-Directory (level 0) <br/>
	 * Key for a fence entry is <b>u</b>.
	 */
	private IntegerMap<FenceEntry> root;
	/**
	 * Global order
	 */
	private final ITF_Filter filter;
	/**
	 * Join threshold used in this tree
	 */
	private final double theta;
	/**
	 * The number of records in this tree
	 */
	private int size;
	/**
	 * Flag to designate similarity function
	 */
	public static boolean exactAnswer = false;

	/**
	 * Number of fence entries
	 */
	public long FEsize = 0;
	/**
	 * Number of leaf entries
	 */
	public long LEsize = 0;
	/**
	 * Number of signatures
	 */
	public long sigsize = 0;
	/**
	 * Comparator of Pair<T>
	 */
	private final Comparator<Pair<T>> pairComparator;
	private final Comparator<T> TComparator;

	public long verifyCount = 0;

	/**
	 * Construct a SI-Tree
	 */

	private Validator checker = null;

	public SI_Tree_Original( double theta, ITF_Filter filter, Validator checker ) {
		this.checker = checker;

		root = new IntegerMap<FenceEntry>();
		this.theta = theta;
		this.filter = filter;
		pairComparator = new Comparator<Pair<T>>() {
			@Override
			public int compare( Pair<T> o1, Pair<T> o2 ) {
				int cmp = Integer.compare( o1.rec1.getID(), o2.rec1.getID() );
				if( cmp == 0 )
					return Integer.compare( o1.rec2.getID(), o2.rec2.getID() );
				return cmp;
			}
		};
		TComparator = new Comparator<T>() {
			@Override
			public int compare( T o1, T o2 ) {
				return Integer.compare( o1.getID(), o2.getID() );
			}
		};
	}

	public SI_Tree_Original( double theta, ITF_Filter filter, List<T> tableT, Validator checker ) {
		this( theta, filter, checker );
		for( T rec : tableT )
			add( rec );
	}

	/**
	 * Add an entry into this SI-Tree
	 *
	 * @param rec
	 *            The record to add
	 */
	public void add( T rec ) {
		int u = rec.getMinLength();
		if( !root.containsKey( u ) )
			root.put( u, new FenceEntry( u ) );
		root.get( u ).add( rec );
		++size;
	}

	/**
	 * Algorithm 3 in the paper <br/>
	 * Retrieve all the candidate pairs
	 */
	public HashSet<Pair<T>> getCandidates( SI_Tree_Original<T> o, double threshold ) {
		// Line 1 : Initialize
		HashSet<Pair<T>> results = new HashSet<Pair<T>>();

		// Line 2 : For all the combinations of fence entries
		for( FenceEntry fe_this : root.values() ) {
			for( FenceEntry fe_other : o.root.values() ) {
				// Line 3 : Check if this fence entry pair can generate any
				// candidate pair
				double cut = threshold * Math.max( fe_this.u, fe_other.u );
				if( Math.min( fe_this.v, fe_other.v ) < cut )
					continue;

				// Line 4 : For all the combinations of leaf entries
				for( LeafEntry le_this : fe_this.P.values() ) {
					for( LeafEntry le_other : fe_other.P.values() ) {
						// Line 5 : Check if this leaf entry pair can generate
						// any candidate pair
						if( Math.min( le_this.t, le_other.t ) < cut )
							continue;

						// Line 6 : Find all the overlapping signatures
						for( int sig : le_this.P.keySet() ) {
							if( !le_other.P.containsKey( sig ) )
								continue;

							// Line 7 : get L_s
							ArrayList<T> Ls = le_this.P.get( sig );
							// Line 8 : get L_t
							ArrayList<T> Lt = le_other.P.get( sig );

							// Line 9~10 : Add candidate pair
							for( T rec1 : Ls )
								for( T rec2 : Lt )
									results.add( new Pair<T>( rec1, rec2 ) );
						}
					}
				}
			}
		}

		return results;
	}

	static final boolean verbose = false;

	public List<Pair<T>> join( SI_Tree_Original<T> o, double threshold ) {
		return joinByEnumeration( o, threshold );
	}

	/**
	 * Algorithm 3 in the paper <br/>
	 * Almost same as {@link #getCandidates(SI_Tree_Original, double) getCandidates}
	 * function, but additionally do join to reduce memory consumption.
	 * This function computes Cartesian product of records in each signature.
	 */
	public List<Pair<T>> joinByCartesian( SI_Tree_Original<T> o, double threshold ) {
		// Line 1 : Initialize
		List<Pair<T>> results = new ArrayList<Pair<T>>();
		// Union?섎뒗 set???됯퇏 媛쒖닔 諛??숈떆??union?섎뒗 set??媛쒖닔

		@SuppressWarnings( "unused" )
		long set_union_count = 0;
		@SuppressWarnings( "unused" )
		long set_union_sum = 0;
		@SuppressWarnings( "unused" )
		long set_union_setsize_sum = 0;

		// Number of comparisions
		verifyCount = 0;

		// Line 2 : For all the combinations of fence entries
		for( FenceEntry fe_this : root.values() ) {
			for( FenceEntry fe_other : o.root.values() ) {
				// Line 3 : Check if this fence entry pair can generate any
				// candidate pair
				double cut = threshold * Math.max( fe_this.u, fe_other.u );
				if( Math.min( fe_this.v, fe_other.v ) < cut )
					continue;

				// Line 4 : For all the combinations of leaf entries
				for( LeafEntry le_this : fe_this.P.values() ) {
					for( LeafEntry le_other : fe_other.P.values() ) {
						// Line 5 : Check if this leaf entry pair can generate
						// any candidate pair
						if( Math.min( le_this.t, le_other.t ) < cut )
							continue;

						List<List<Pair<T>>> candidates = new ArrayList<List<Pair<T>>>();

						StringBuilder builder = new StringBuilder();
						if( verbose )
							builder.append( "Join between: " );
						// Line 6 : Find all the overlapping signatures
						for( int sig : le_this.P.keySet() ) {
							if( !le_other.P.containsKey( sig ) )
								continue;

							List<Pair<T>> sig_candidates = new ArrayList<Pair<T>>();
							// Line 7 : get L_s
							ArrayList<T> Ls = le_this.P.get( sig );
							// Line 8 : get L_t
							ArrayList<T> Lt = le_other.P.get( sig );

							if( verbose )
								builder.append( "Sig-" + sig + "=>" + Ls.size() + "x" + Lt.size() + " " );

							// if(Ls.size() != 1 && Lt.size() != 1)
							// System.out.println(Ls.size() + "*" + Lt.size());
							// count += Ls.size() * Lt.size();

							try {
								// Line 9~10 : Add candidate pair
								for( T rec1 : Ls )
									for( T rec2 : Lt )
										sig_candidates.add( new Pair<T>( rec1, rec2 ) );

								// for (T rec1 : Ls) {
								// Set<T.Expanded> exp1 = null;
								// if (exactAnswer) exp1 = (Set<T.Expanded>) rec1.generateAll();
								// for (T rec2 : Lt) {
								// Pair<T> sirp = new Pair<T>(rec1, rec2);
								// if (evaled.contains(sirp)) ++duplicate_results;
								// // else
								// // evaled.add(sirp);
								// // Similarity check
								// double sim = 0;
								// if (exactAnswer) {
								// Set<T.Expanded> exp2 = (Set<T.Expanded>) rec2.generateAll();
								// for (T.Expanded exp1R : exp1)
								// for (T.Expanded exp2R : exp2)
								// sim = Math.max(sim, exp1R.similarity(exp2R));
								// } else
								// sim = rec1.similarity(rec2);
								// if (sim >= threshold) results.add(sirp);
								// ++count;
								// }
								// Collections.sort(sig_candidates);
								set_union_setsize_sum += sig_candidates.size();
								candidates.add( sig_candidates );
							}
							catch( OutOfMemoryError e ) {
								if( verbose )
									System.out.println( builder.toString() );
								throw e;
							}
						}
						List<Pair<T>> union_candidates = StaticFunctions.union( candidates, pairComparator );
						if( verbose )
							System.out.println( " Union size:" + union_candidates.size() );
						System.out.println( builder.toString() );
						++set_union_count;
						set_union_sum += candidates.size();
						// results.addAll(union_candidates);
						if( skipEquiCheck )
							continue;
						for( Pair<T> p : union_candidates ) {
							T rec1 = p.rec1;
							T rec2 = p.rec2;
							double sim = 0;
							sim = rec1.similarity( rec2, checker );
							if( sim >= threshold )
								results.add( p );
							++verifyCount;
						}
					}
				}
			}
		}
		if( DEBUG.SIJoinON ) {
			System.out.println( "Comparisons : " + verifyCount );
			System.out.println( "set_union_count: " + set_union_count );
			System.out.println( "set_union_sum: " + set_union_sum );
			System.out.println( "set_union_setsize_sum: " + set_union_setsize_sum );
		}
		return results;
	}

	/**
	 * Algorithm 3 in the paper <br/>
	 * Almost same as {@link #getCandidates(SI_Tree_Original, double) getCandidates}
	 * function, but additionally do join to reduce memory consumption.
	 * This function enumerate signatures for each record in R and then find
	 * overlapping records.
	 */
	public List<Pair<T>> joinByEnumeration( SI_Tree_Original<T> o, double threshold ) {
		// Line 1 : Initialize
		List<Pair<T>> results = new ArrayList<Pair<T>>();
		// Union?섎뒗 set???됯퇏 媛쒖닔 諛??숈떆??union?섎뒗 set??媛쒖닔
		long set_union_count = 0;
		long set_union_sum = 0;
		long set_union_setsize_sum = 0;
		// Number of comparisions
		verifyCount = 0;

		// Line 2 : For all the combinations of fence entries
		for( FenceEntry fe_this : root.values() ) {
			for( FenceEntry fe_other : o.root.values() ) {
				// Line 3 : Check if this fence entry pair can generate any
				// candidate pair
				double cut = threshold * Math.max( fe_this.u, fe_other.u );
				if( Math.min( fe_this.v, fe_other.v ) < cut )
					continue;

				// Line 4 : For all the combinations of leaf entries
				for( LeafEntry le_this : fe_this.P.values() ) {
					for( LeafEntry le_other : fe_other.P.values() ) {
						// Line 5 : Check if this leaf entry pair can generate
						// any candidate pair
						if( Math.min( le_this.t, le_other.t ) < cut )
							continue;

						Set<T> evaluated = new HashSet<T>();

						// Line 6 : For each record in R, find all the overlapping
						// signatures
						for( int sig : le_this.P.keySet() ) {
							if( !le_other.P.containsKey( sig ) )
								continue;
							// Line 7 : get L_s
							List<T> Ls = le_this.P.get( sig );
							for( T rec1 : Ls ) {
								// If this record is already evaluated, skip
								if( evaluated.contains( rec1 ) )
									continue;
								evaluated.add( rec1 );
								Set<Integer> signatures = rec1.getSignatures( filter, theta );
								List<List<T>> candidates = new ArrayList<List<T>>();
								for( int sig2 : signatures ) {
									// Line 8 : get L_t
									List<T> Lt = le_other.P.get( sig2 );
									// Line 9~10 : Add candidate pair
									if( Lt != null && !Lt.isEmpty() )
										candidates.add( Lt );
								}
								List<T> union_candidates = StaticFunctions.union( candidates, TComparator );
								++set_union_count;
								set_union_sum += candidates.size();
								// results.addAll(union_candidates);
								if( skipEquiCheck )
									continue;
								for( T t : union_candidates ) {
									double sim = 0;
									sim = rec1.similarity( t, checker );
									if( sim >= threshold ) {
										if ( rec1.getID() != t.getID() ) SimilarityFunc.selectiveExp( (SIRecord) rec1, (SIRecord) t, true );
										results.add( new Pair<T>( rec1, t ) );
									}
									++verifyCount;
								}
							}
						}
					}
				}
			}
		}
		System.out.println( "Comparisons : " + verifyCount );
		System.out.println( "set_union_count: " + set_union_count );
		System.out.println( "set_union_sum: " + set_union_sum );
		System.out.println( "set_union_setsize_sum: " + set_union_setsize_sum );
		return results;
	}

	/**
	 * Self-join version of Algorithm 3 in the paper <br/>
	 * Almost same as {@link #getCandidates(SI_Tree_Original, double) getCandidates}
	 * function, but additionally do join to reduce memory consumption.
	 */
	@SuppressWarnings( "unchecked" )
	public HashSet<Pair<T>> selfjoin( double threshold ) {
		// Line 1 : Initialize
		HashSet<Pair<T>> results = new HashSet<Pair<T>>();
		long count = 0;
		long duplicate_results = 0;

		// Line 2 : For all the combinations of fence entries
		for( FenceEntry fe_this : root.values() ) {
			for( FenceEntry fe_other : root.values() ) {
				// Line 3 : Check if this fence entry pair can generate any
				// candidate pair
				double cut = threshold * Math.max( fe_this.u, fe_other.u );
				if( Math.min( fe_this.v, fe_other.v ) < cut )
					continue;

				// Line 4 : For all the combinations of leaf entries
				for( LeafEntry le_this : fe_this.P.values() ) {
					for( LeafEntry le_other : fe_other.P.values() ) {
						// Line 5 : Check if this leaf entry pair can generate
						// any candidate pair
						if( Math.min( le_this.t, le_other.t ) < cut )
							continue;

						HashSet<Pair<T>> evaled = new HashSet<Pair<T>>();
						// Line 6 : Find all the overlapping signatures
						for( int sig : le_this.P.keySet() ) {
							if( !le_other.P.containsKey( sig ) )
								continue;

							// Line 7 : get L_s
							ArrayList<T> Ls = le_this.P.get( sig );
							// Line 8 : get L_t
							ArrayList<T> Lt = le_other.P.get( sig );

							// if(Ls.size() != 1 && Lt.size() != 1)
							// System.out.println(Ls.size() + "*" + Lt.size());
							// count += Ls.size() * Lt.size();

							// // Line 9~10 : Add candidate pair
							for( T rec1 : Ls ) {
								int id1 = rec1.getID();
								Set<T.Expanded> exp1 = null;
								if( exactAnswer )
									exp1 = (Set<T.Expanded>) rec1.generateAll();
								for( T rec2 : Lt ) {
									int id2 = rec2.getID();
									if( id1 <= id2 )
										continue;
									Pair<T> sirp = new Pair<T>( rec1, rec2 );
									if( evaled.contains( sirp ) ) {
										++duplicate_results;
										continue;
									}
									else
										evaled.add( sirp );
									// Similarity check
									double sim = 0;
									if( exactAnswer ) {
										Set<T.Expanded> exp2 = (Set<T.Expanded>) rec2.generateAll();
										for( T.Expanded exp1R : exp1 )
											for( T.Expanded exp2R : exp2 )
												sim = Math.max( sim, exp1R.similarity( exp2R, checker ) );
									}
									else
										sim = rec1.similarity( rec2, checker );
									if( sim >= threshold )
										results.add( sirp );
									++count;
								}
							}
						}
					}
				}
			}
		}

		System.out.println( "Comparisons : " + count );
		System.out.println( "Duplicate results : " + duplicate_results );
		return results;
	}

	/**
	 * Naive version of SI-join. <br/>
	 * Only one record set is indexed and records in the other set is expanded
	 * in runtime. <br/>
	 * Note that this method should be used if threshold == 1
	 */
//	@SuppressWarnings( "unchecked" )
//	public HashSet<Pair<T>> naivejoin( List<T> tableS, boolean is_selfjoin ) {
//		HashSet<Pair<T>> results = new HashSet<Pair<T>>();
//		long count = 0;
//		for( T rec : tableS ) {
//			int id1 = rec.getID();
//			Set<T.Expanded> expanded = (Set<T.Expanded>) rec.generateAll();
//			for( T.Expanded exp : expanded ) {
//				RecordInterface rec1 = exp.toRecord();
//				Set<Integer> sig = rec1.getSignatures( filter, 1 );
//				// Number of sig must be 1
//				if( sig.size() != 1 )
//					throw new RuntimeException();
//				// For all fence entries
//				for( FenceEntry fe : root.values() ) {
//					// Check length condition
//					if( fe.v < exp.size() || exp.size() < fe.u )
//						continue;
//					// For all leaf entries
//					for( LeafEntry le : fe.P.values() ) {
//						// Check length condition
//						if( le.t < exp.size() )
//							continue;
//						Integer key = sig.iterator().next();
//						ArrayList<T> values = le.P.getI( key );
//						if( values == null )
//							continue;
//						// Check if similarity equals to 1
//						for( T rec2 : values ) {
//							int id2 = rec2.getID();
//							if( is_selfjoin && id1 <= id2 )
//								break;
//							// Similarity check
//							double sim = rec1.similarity( rec2, checker );
//							if( sim == 1 ) {
//								Pair<T> sirp = new Pair<T>( rec, rec2 );
//								results.add( sirp );
//							}
//							++count;
//						}
//					}
//				}
//			}
//		}
//
//		System.out.println( "Comparisons : " + count );
//		return results;
//	}

	public int size() {
		return size;
	}

	/**
	 * Fence-entry for S-Directory (level 1)<br/>
	 * Contains three fields &lt;u, v, P&gt;<br/>
	 * u : The number of tokens of a string<br/>
	 * v : The maximal number of tokens in the full expanded sets of strings
	 * whose length is u<br/>
	 * P : A set of pointers to the leaf nodes
	 */
	private class FenceEntry {
		/**
		 * The number of tokens of a string
		 */
		private final int u;
		/**
		 * The maximal number of tokens in the full expanded sets of strings
		 * whose length is u
		 */
		private int v;
		/**
		 * A set of pointers to the leaf nodes
		 */
		private IntegerMap<LeafEntry> P;

		FenceEntry( int u ) {
			this.u = v = u;
			P = new IntegerMap<LeafEntry>();
			++FEsize;
		}

		/**
		 * Add a record to some leaf node under this fence entry
		 *
		 * @param rec
		 */
		void add( T rec ) {
			int v = rec.getMaxLength();
			this.v = Math.max( v, this.v );
			if( !P.containsKey( v ) )
				P.put( v, new LeafEntry( v ) );
			P.get( v ).add( rec );
		}
	}

	/**
	 * Leaf-entry for S-Directory (level 2)<br/>
	 * Contains two fields &lt;t, P&gt;<br/>
	 * t : An integer to denote the number of the tokens in the full expanded
	 * set of a string <br/>
	 * P : A pointer to an inverted list
	 */
	private class LeafEntry {
		/**
		 * An integer to denote the number of the tokens in the full expanded
		 * set of a string
		 */
		private int t;
		/**
		 * A pointer to an inverted index
		 */
		private IntegerMap<ArrayList<T>> P;

		LeafEntry( int v ) {
			t = v;
			P = new IntegerMap<ArrayList<T>>();
			++LEsize;
		}

		/**
		 * Add a record to this leaf node.
		 *
		 * @param rec
		 */
		void add( T rec ) {
			// Special code for theta == 1 and filter == 1
			if( theta == 1 ) {
			}
//			System.out.println(rec.getID()+"\t"+rec);
			Collection<Integer> signature = rec.getSignatures( filter, theta );
			if ( signature == null ) return;
			// Add to inverted indices
			for( int sig : signature ) {
				if( !P.containsKey( sig ) )
					P.put( sig, new ArrayList<T>( 5 ) );
				P.get( sig ).add( rec );
			}
			sigsize += signature.size();
		}
	}
}
