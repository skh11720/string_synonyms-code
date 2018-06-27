package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.delta.QGramDeltaGenerator;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.PosQGramFilterDPDelta;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.PosQGram;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.Util;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PosQGramFilterDPDeltaTest {
	
	private static Query query;
	private static int q;
	
	@BeforeClass
	public static void initialize() throws IOException {
		query = TestUtils.getTestQuery();
		q = 3;
	}
	
	@Test
	public void testExistence() {
		int n = 100;
		for ( q=1; q<=3; ++q ) {
			for ( int deltaMax=0; deltaMax<3; ++deltaMax ) {
				QGramDeltaGenerator qdgen = new QGramDeltaGenerator( q, deltaMax );

				ObjectOpenHashSet<PosQGram> candSet_pqgrams = new ObjectOpenHashSet<>();
				Map<Record, Set<PosQGram>> answerMap = new Object2ObjectOpenHashMap<>();
				// Get a set of candidate pos qgrams and the answers.
				for ( int i=0; i<n; ++i ) {
					Record x = query.searchedSet.getRecord( i );
					Set<PosQGram> answerSet = new ObjectOpenHashSet<>();
					boolean debug = false;
//					if ( x.getID() == 5 ) debug = true;
					for ( Record x_exp : x.expandAll() ) {
						if (debug) System.out.println( "x_exp: "+Arrays.toString( x_exp.getTokensArray() ) );
						List<List<QGram>> cand_pqgrams = x_exp.getSelfQGrams( q+deltaMax, x_exp.size() );
						for ( int k=0; k<cand_pqgrams.size(); ++k ) {
							for ( QGram qgram : cand_pqgrams.get( k ) ) {
								if (debug) System.out.println( "\toriginal pqgram: "+qgram+", "+k);
								for ( Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram ) ) {
									PosQGram pqgram = new PosQGram( entry.getKey(), k );
									answerSet.add( pqgram );
									candSet_pqgrams.add( pqgram );
									if (debug) System.out.println( "\t\tpqgram-delta: "+pqgram );
								}
							}
						}
					}
					answerMap.put( x, answerSet );
					if (debug) {
						SampleDataTest.inspect_record( x, query, q );
						System.out.println( "answerSet: " );
						for ( PosQGram pqgram : answerSet ) System.out.println( pqgram );
					}
				}

		//		for ( Entry<Record, Set<PosQGram>> entry : answer.entrySet() ) {
		//			SampleDataTest.inspect_record( entry.getKey(), query, q );
		//			System.out.println( "pos_qgram_delta:" );
		//			for ( PosQGram pqgram : entry.getValue() ) {
		//				System.out.println( pqgram );
		//			}
		//		}
				
				for ( int i=0; i<n; ++i ) {
					Record x = query.searchedSet.getRecord( i );
					PosQGramFilterDPDelta filter = new PosQGramFilterDPDelta( x, q, deltaMax );
					for ( PosQGram pqgram : candSet_pqgrams ) {
//						q: 1, deltaMax: 2
//						record: 19, [55504, 4272, 2486]
//						transLen: [2, 5]
//						pqgram: [2147483647 , 0]

//						if ( x.getID() != 19 || !pqgram.equals( new PosQGram(new int[] {2147483647}, 0) ) ) continue;
//						SampleDataTest.inspect_record( x, query, q );
						boolean answer = answerMap.get( x ).contains( pqgram );
						if ( pqgram.qgram.qgram[0] == Integer.MAX_VALUE ) {
							for ( int k=pqgram.pos; k>0; --k ) answer |= answerMap.get( x ).contains( new PosQGram( pqgram.qgram, k ) );
						}
						boolean filter_out = filter.existence( pqgram.qgram, pqgram.pos );
//						System.out.println( "q: "+q+", deltaMax: "+deltaMax );
//						System.out.println( "record: "+x.getID()+", "+Arrays.toString( x.getTokensArray() ) );
//						System.out.println( "transLen: "+Arrays.toString( x.getTransLengths() ) );
//						System.out.println( "pqgram: "+pqgram );
//						System.out.println( "answer: "+answer );
//						System.out.println( "filter: "+filter_out );
//						filter.printBTransLen();
//						filter.printBGen();
						assertEquals( answer, filter_out );
					}
				}
			} // end for deltaMax
		} // end for q
	} // end testExistence
	
	@Test
	public void testAlignWithSeq() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {  // ( int[] pat, int start, int end, int[] seq, int delta ) {
		int deltaMax = 2;
		final int INF = PosQGramFilterDPDelta.INF;
		
		int[] seq = {10, 20, 30, 40};
		int[][] pat_list =  {
				{10, 20, 30, 40},
				{10, 20, 30},
				{20, 30, 40},
				{10, 30, 40},
				{10, 20}, 
				{10, 30},
				{10, 40},
				{20, 30},
				{20, 40},
				{30, 40},
				{10},
				{-10, -20, -30, -40},
				{-10, -20, -30, -40, -50},
		};
		int[][] answer_list = {
//				{0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 8, 9},
//				{1, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 7, 8},
//				{1, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 7, 8},
				// delta=2
				{0, 1, 1, 1, 2, 2, 2, 2, 2, 2, INF, INF, INF},
				{1, 2, 2, 2, INF, INF, INF, INF, INF, INF, INF, INF, INF},
				{1, 2, 2, 2, INF, INF, INF, INF, INF, INF, INF, INF, INF},
				// delta=3
				{0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, INF, INF},
				{1, 2, 2, 2, 3, 3, 3, 3, 3, 3, INF, INF, INF},
				{1, 2, 2, 2, 3, 3, 3, 3, 3, 3, INF, INF, INF},
		};
		
		for ( int d=2; d<4; ++d ) {
			PosQGramFilterDPDelta target = new PosQGramFilterDPDelta( query.searchedSet.getRecord( 0 ), q, d );
			Method method = PosQGramFilterDPDelta.class.getDeclaredMethod( "alignWithSeq", int[].class, int.class, int.class, int[].class );
			method.setAccessible( true );
			for ( int i=0; i<pat_list.length; ++i ) {
				int[] pat = pat_list[i];
				assertEquals( answer_list[(d-2)*3+0][i], (method.invoke( target, pat, 0, pat.length, seq )) );
				assertEquals( answer_list[(d-2)*3+1][i], (method.invoke( target, pat, 1, pat.length, seq )) );
				assertEquals( answer_list[(d-2)*3+2][i], (method.invoke( target, pat, 0, pat.length-1, seq )) );
			}
		}
	}
	
	@Test
	public void testAlignWithSuffix() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		int deltaMax = 3;
		PosQGramFilterDPDelta target = new PosQGramFilterDPDelta( query.searchedSet.getRecord( 0 ), q, deltaMax );
		Method method = PosQGramFilterDPDelta.class.getDeclaredMethod( "alignWithSuffix", int[].class, int.class, int.class, int[].class, int.class );
		method.setAccessible( true );
		int[] seq = {10, 20, 30, 40, 50, 60, 70, 80, 90};
		int[][] pat_list =  {
				{10, 20, 30, 40, 50, 60, 70, 80, 90},
				{90},
				{80},
				{50, 60, 70, 80, 90},
				{50, 60, 70, 80},
				{50, 60, 80, 90},
				{40, 60, 80, 90},
				{40, 50, 60, 70},
				{30, 40, 70, 80, 90},
				{30, 50, 60, 80, 90},
				{30, 70, 80, 90},
				{30, 50, 70, 90},
				{-10, -20, -30, -40},
		};
		int[][] answer_list = {
				// start =0, end = pat.length
				{0, 8, -1, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // d=0
				{0, 8, 7, 4, 4, 4, -1, -1, -1, -1, -1, -1, -1}, // d=1
				{0, 8, 7, 4, 4, 4, 3, 3, 2, 2, -1, -1, -1}, // d=2
				// start = 1
				{1, 9, 9, 5, -1, -1, -1, -1, -1, -1, 6, -1, -1}, // d=0
				{1, 9, 9, 5, 5, 5, 5, -1, -1, 4, 6, -1, -1}, // d=1
				{1, 9, 9, 5, 5, 5, 5, 4, 3, 4, 6, 4, -1}, // d=2
		};
		for ( int d=0; d<3; ++d ) {
			for ( int i=0; i<pat_list.length; ++i ) {
				int[] pat = pat_list[i];
				assertEquals( answer_list[d][i], (int)(method.invoke(target, pat, 0, pat.length, seq, d)) );
//				System.out.println( Arrays.toString( pat )+", "+answer_list[3+d][i] );
				assertEquals( answer_list[3+d][i], (int)(method.invoke(target, pat, 1, pat.length, seq, d)) );
			}
		}
	}
	
	@Test
	public void testAlignWithPrefix() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//		int deltaMax = 2;
		final int INF = PosQGramFilterDPDelta.INF;
		int[] seq = {10, 20, 30, 40, 50, 60, 70, 80, 90};
		int[][] pat_list =  {
				{10, 20, 30, 40, 50, 60, 70, 80, 90},
				{10, 20, 30, 40, 50, 60, 70, 80, 90, 100},
				{0, 10, 20, 30, 40, 50, 60, 70, 80, 90},
				{10},
				{20},
				{10, 20, 30, 40, 50},
				{20, 30, 40, 50},
				{10, 20, 40, 50},
				{10, 30, 50, 60},
				{30, 40, 50, 60},
				{10, 20, 50, 60, 70},
				{10, 30, 40, 60, 70},
				{10, 10, 20, 30},
				{10, 10, 20, 40},
				{10, 10, 30, 50},
				{-10, -20, -30, -40},
		};

		int[][] answer_list = {
//				{0, INF, INF, 0, 1, 0, 1, 1, 2, 2, 2, 2, INF, INF, INF, INF},
//				{0, INF, 0, 0, 0, INF, INF, INF, INF, INF, INF, INF, 0, 1, 2, INF}
				
				// pat_start = 0, seq_start = 0
				{0, INF, INF, 0, INF, 0, INF, INF, INF, INF, INF, INF, INF, INF, INF, INF}, // d = 0
				{0, INF, INF, 0, 1, 0, 1, 1, INF, INF, INF, INF, INF, INF, INF, INF}, // d = 1
				{0, INF, INF, 0, 1, 0, 1, 1, 2, 2, 2, 2, INF, INF, INF, INF}, // d = 2
				// pat_start = 1, seq_start = 0
				{INF, INF, 0, 0, 0, INF, INF, INF, INF, INF, INF, INF, 0, INF, INF, INF}, // d = 0
				{1, INF, 0, 0, 0, 1, INF, INF, INF, INF, INF, INF, 0, 1, INF, INF}, // d = 1
				{1, INF, 0, 0, 0, 1, 2, 2, INF, INF, INF, INF, 0, 1, 2, INF}, // d = 2
				// pat_start = 0, seq_start = 1
				{INF, INF, INF, INF, 0, INF, 0, INF, INF, INF, INF, INF, INF, INF, INF, INF}, // d = 0
				{INF, INF, INF, INF, 0, INF, 0, INF, INF, 1, INF, INF, INF, INF, INF, INF}, // d = 1
				{INF, INF, INF, INF, 0, INF, 0, INF, INF, 1, INF, INF, INF, INF, INF, INF}, // d = 2
				// pat_start = 1, seq_start = 1
				{0, INF, INF, 0, 0, 0, INF, INF, INF, INF, INF, INF, INF, INF, INF, INF}, // d = 0
				{0, INF, INF, 0, 0, 0, 1, 1, INF, INF, INF, INF, INF, INF, INF, INF}, // d = 1
				{0, INF, INF, 0, 0, 0, 1, 1, 2, 2, 2, 2, INF, INF, INF, INF}, // d = 2
		};
		
		for ( int d=0; d<3; ++d ) {
			PosQGramFilterDPDelta target = new PosQGramFilterDPDelta( query.searchedSet.getRecord( 0 ), q, d );
			Method method = PosQGramFilterDPDelta.class.getDeclaredMethod( "alignWithPrefix", int[].class, int.class, int.class, int[].class, int.class );
			method.setAccessible( true );
			for ( int i=0; i<pat_list.length; ++i ) {
				int[] pat = pat_list[i];
//				System.out.println( "0, "+Arrays.toString( pat )+", "+d );
				assertEquals( answer_list[d][i], (int)(method.invoke(target, pat, 0, pat.length, seq, 0 )) );
//				System.out.println( "1, "+Arrays.toString( pat )+", "+d );
//				System.out.println( (method.invoke(target, pat, 1, pat.length, seq)) );
				assertEquals( answer_list[3+d][i], (int)(method.invoke(target, pat, 1, pat.length, seq, 0 )) );
				assertEquals( answer_list[6+d][i], (int)(method.invoke(target, pat, 0, pat.length, seq, 1 )) );
				assertEquals( answer_list[9+d][i], (int)(method.invoke(target, pat, 1, pat.length, seq, 1 )) );
			}
		}
	}
}
