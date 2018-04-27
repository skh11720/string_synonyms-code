package snu.kdd.synonym.synonymRev.algorithm.misc;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StopWatch;


class PosQGramFilterDP {
	
	private final Record record;
	private final int q;
	private Boolean[][] bTransLen;
	// bTransLen[i][l] indicates that s[1,i] can be transformed to a string of length l.
	
	public PosQGramFilterDP(final Record record, final int q) {
		this.record = record;
		this.q = q;
		computeTransformedLength();
	}
	
	public Boolean existence(final QGram qgram, final int k) {
		/*
		 * Return true if the pos q-gram [qgram, k] is in TPQ of this.record; otherwise return false.
		 * Use dynamic programming to compute the matrix bGen
		 * whose element bGen[i][j] indicates that [qgram[1,j], k] is in TPQ of this.record[1,i].
		 * The return value is equal to bGen[record.size()][q].
		 */
		
		final Boolean debug = false;
		
		// trivial case
		if (record.getMaxTransLength() <= k ) return false;
		
		/*
		 * initialize:
		 * bGen[0][i][j] is true iff there is a transformed string of s[1,i] which ends with g[1,j] and "generates" [g[1,j], k].
		 * bGen[1][i][j] is true iff there is a transformed string o s[1,i] which "generates" [g[1,j], k].
		 */
		Boolean[][][] bGen = new Boolean[2][record.size()+1][q+1];
		for (int i=0; i<=record.size(); i++) Arrays.fill(  bGen[0][i], false );
		for (int i=0; i<=record.size(); i++) Arrays.fill(  bGen[1][i], false );
		
		// bottom-up recursion
		for (int i=1; i<=record.size(); i++) {
			for (int j=1; j<=q; j++) {

				if (i == record.size() && qgram.qgram[j-1] == Integer.MAX_VALUE) {
					bGen[0][i][j] = bGen[1][i][j] = bGen[0][i][j-1];
					continue;
				}

				bGen[0][i][j] |= recursion0( qgram, k, i, j, bGen );
				bGen[1][i][j] |= recursion1( qgram, k, i, j, bGen );
			}
		}
		if (debug) System.out.println( "["+Arrays.toString( qgram.qgram )+", "+k+"]" );
		if (debug) System.out.println(Arrays.deepToString(bTransLen).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[0]).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(  );
		if (debug) System.out.println(Arrays.deepToString(bGen[1]).replaceAll( "],", "]\n" ));
		return bGen[1][record.size()][q];
	}
	
	public Boolean getBTransLen(final int i, final int l) {
		if (l < 0) return false;
		else return bTransLen[i][l];
	}
	
	public Boolean recursion0(final QGram qgram, final int k, final int i, final int j, final Boolean[][][] bGen) {
		/*
		 * Compute bGen[0][i][j] and return the result.
		 */

		for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
			int i_back = i - rule.leftSize();
			assert i_back >= 0;
			
			// Case 0-1
			// TODO: can be improved
			for (int j_start=0; j_start<j; j_start++) {
				if ( Arrays.equals( Arrays.copyOfRange( qgram.qgram, j_start, j ), rule.getRight() ) ) if (bGen[0][i_back][j_start]) return true;
			}
			
			// Case 0-2
			if (isSuffixOf( qgram.qgram, 0, j, rule.getRight())) if(getBTransLen(i_back, k - (rule.rightSize() - j) )) return true;
		}
		return false;
	}
	
	public Boolean recursion1(final QGram qgram, final int k, final int i, final int j, final Boolean[][][] bGen) {
		/*
		 * Compute bGen[1][i][j] and return the result.
		 */

		for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
			int i_back = i - rule.leftSize();
			assert i_back >= 0;
			
			// Case 1-1
			if (bGen[0][i][j]) return true;
			
			// Case 1-2
			if (bGen[1][i_back][j]) return true;
			
			// Case 1-3
			// TODO: can be improved
			for (int j_start=0; j_start<j; j_start++) {
				if ( isPrefixOf( qgram.qgram, j_start, j, rule.getRight() ) ) 
					if (bGen[0][i_back][j_start]) return true;
			}
			
			// Case 1-4
			int start = isSubstringOf( qgram.qgram, j, rule.getRight());
			if (start != -1 && getBTransLen(i_back, k-start)) return true;
	//		if (debug) System.out.println( "case 3: "+bGen[1][i][j]+"\t"+start);
	//		if (debug) System.out.println( Arrays.toString( qgram.qgram )+"\t"+Arrays.toString( rule.getRight() ));
		}
		return false;
	}
	
	public void testIsSubstringOf() {
		System.out.println( "test PosQGramFilterDP.isSubstringOf()" );
		Random random = new Random();
		final int n_max = 10;
		final int nRepeat = 1000;
		for (int idx_case=0; idx_case<nRepeat; idx_case++) {
			final int len = random.nextInt(n_max)+1;
			int[] pat = new int[len];
			for (int i=0; i<len; ++i) pat[i] = random.nextInt( n_max );
			final int len_pre = random.nextInt( n_max );
			final int len_suf = random.nextInt( n_max );
			final int len_pos = len + len_pre + len_suf;
			final int len_neg = random.nextInt(n_max)+1;
			int[] seq_pos = new int[len+len_pre+len_suf];
			for (int i=0; i<len_pos; i++) {
				if (i >= len+len_pre || i < len_pre) seq_pos[i] = random.nextInt( n_max ) + n_max;
				else seq_pos[i] = pat[i-len_pre];
			}
			int[] seq_neg = new int[len_neg];
			for (int i=0; i<len_neg; i++) seq_neg[i] = random.nextInt( n_max ) + n_max;

//			System.out.println( len_pre+", "+len+", "+len_suf );
//			System.out.println( Arrays.toString( pat ) );
//			System.out.println( Arrays.toString( seq_pos ) );
//			System.out.println( Arrays.toString( seq_neg ) );
//			System.out.println( isSubstringOf( pat, seq_pos ) );
//			System.out.println( isSubstringOf( pat, seq_neg ) );
			
			assert isSubstringOf( pat, pat.length, seq_pos ) == len_pre;
			assert isSubstringOf( pat, pat.length, seq_neg ) == -1;
		}
	}
	
	public void testIsPrefixOf() {
		System.out.println( "test PosQGramFilterDp.isPrefixOf()" );
		Random random = new Random();
		final int n_max = 10;
		final int nRepeat = 1000;
		for (int idx_case=0; idx_case<nRepeat; idx_case++) {
			final int len_seq = random.nextInt( n_max )+1;
			int[] seq = new int[len_seq];
			for (int i=0; i<len_seq; i++) seq[i] = random.nextInt( n_max );

			final int len_pat = Math.min( random.nextInt( n_max )+1, len_seq);
			final int start = random.nextInt( n_max )+1;
			final int end = random.nextInt( n_max )+1;
			int[] pat = new int[start + len_pat + end];
			for (int i=0; i<start+len_pat+end; i++) {
				if (i < start || i >= start+len_pat) pat[i] = random.nextInt( n_max ) + n_max;
				else pat[i] = seq[i-start];
			}
			final int test_start = random.nextInt(start);
			final int test_end = random.nextInt(end)+start+len_pat;

//			System.out.println( len_pat+", "+start+", "+end );
//			System.out.println( Arrays.toString( seq ) );
//			System.out.println( Arrays.toString( pat ) );
//			System.out.println( test_start+", "+test_end );
//			System.out.println( isPrefixOf( pat, start, start+len_pat, seq ) );
//			System.out.println( isPrefixOf( pat, test_start, start+len_pat, seq ) );
//			System.out.println( isPrefixOf( pat, start, start+len_pat+test_end, seq ) );
//			System.out.println( isPrefixOf( pat, test_start, start+len_pat+test_end, seq ) );
			
			assert isPrefixOf( pat, start, start+len_pat, seq ) == true;
			assert isPrefixOf( pat, test_start, start+len_pat, seq ) == false;
			assert isPrefixOf( pat, start, start+len_pat+test_end, seq ) == false;
			assert isPrefixOf( pat, test_start, start+len_pat+test_end, seq ) == false;
		}
	}

	private void computeTransformedLength() {
		/*
		 * compute the matrix bTransLen using dynamic programming.
		 * the time complexity is |s|^2 * |R(s)|.
		 */
		// initialize
		bTransLen = new Boolean[record.size()][];
		for (int i=0; i<record.size(); i++) {
			bTransLen[i] = new Boolean[record.getMaxTransLength()];
			Arrays.fill( bTransLen[i], false );
		}
		bTransLen[0][0] = true;
		
		for (int i=1; i<record.size(); i++) {
			for (int l=1; l<record.getMaxTransLength(); l++ ) {
				for (final Rule rule : record.getSuffixApplicableRules( i-1 )) {
					int i_back = i - rule.leftSize();
					int l_back = l - rule.rightSize();
					//System.out.println( ""+i+", "+j+", "+i_back+", "+l_back+", "+rule.leftSize()+", "+rule.rightSize() );
					if (i_back >= 0 && l_back >= 0) bTransLen[i][l] |= bTransLen[i_back][l_back];
				}
			}
		}
//		System.out.println(Arrays.deepToString(bTransLen).replaceAll( "],", "]\n" ));
	}
	
	private Boolean isPrefixOf(final int[] pat, final int start, final int end, final int[] seq) {
		/*
		 * Return true if pat[start:end] is a prefix of seq; otherwise return false.
		 */
		if (end - start > seq.length ) return false;
		for (int i=start; i<end; i++)
			if (pat[i] != seq[i-start]) return false;
		return true;
	}

	private Boolean isSuffixOf(final int[] pat, final int start, final int end, final int[] seq) {
		/*
		 * Return true if pat[start:end] is a suffix of seq; otherwise return false.
		 */
		if (end - start > seq.length ) return false;
		for (int i=start; i<end; i++)
			if (pat[i] != seq[seq.length+i-end]) return false;
		return true;
	}
	
	private int isSubstringOf(final int[] pat, final int end, final int[] seq) {
		/*
		 * Return the start position of pat[start:end] if pat[start:end] is a substring of seq; otherwise return -1.
		 * Current implementation is naive: takes O(|pat|*|seq|).
		 * TODO: Need to be improved later!! (use AC automata)
		 */
		if (end > seq.length) return -1;
		for (int i=0; i<seq.length-end+1; i++) {
			Boolean res = true;
			for (int j=0; j<end; j++) {
				if (pat[j] != seq[i+j]) res = false;
				if (!res) break;
			}
			if (res) return i;
		}
		return -1;
	}
}

public class PosQGramFilterDPTest {
	
	private static Boolean debug = false;
	
	public static void inspect_record(final Record record, final Query query, final int q) {
		//System.out.println("record: "+record.toString(query.tokenIndex));
		System.out.println("record: "+Arrays.toString(record.getTokensArray()) );

		System.out.println( "applicable rules: " );
		for (int pos=0; pos<record.size(); pos++ ) {
			for (final Rule rule : record.getSuffixApplicableRules( pos )) {
				//System.out.println("\t("+rule.toOriginalString(query.tokenIndex)+", "+pos+")");
				System.out.println("\t("+rule.toString()+", "+pos+")");
			}
		}

		System.out.println( "transformed strings: " );
		final List<Record> expanded = record.expandAll();
		for( final Record exp : expanded ) {
			System.out.println( "\t"+Arrays.toString( exp.getTokensArray() ) );
		}
		
		System.out.println( "positional q-grams: " );
		List<List<QGram>> qgrams_self = record.getSelfQGrams( q, record.getTokenCount() );
		for (int i=0; i<qgrams_self.size(); i++) {
			for (final QGram qgram : qgrams_self.get(i)) {
				//System.out.println( "\t["+qgram.toString( query.tokenIndex )+", "+i+"]" );
				System.out.println( "\t["+qgram.toString()+", "+i+"]" );
			}
		}
		
		System.out.println( "positional q-grams in a transformed string: " );
		List<List<QGram>> qgrams = record.getQGrams(q);
		for (int i=0; i<qgrams.size(); i++) {
			for (final QGram qgram : qgrams.get(i)) {
				//System.out.println( "\t["+qgram.toString( query.tokenIndex )+", "+i+"]" );
				System.out.println( "\t["+qgram.toString()+", "+i+"]" );
			}
		}
	}
	
	public static void check_samples(Query query, ObjectList<Record> sample_records, ObjectSet<Rule> sample_rules, 
			ObjectList<ObjectOpenHashSet<QGram>> sample_pos_qgrams) {
		System.out.println( "records: "+sample_records.size() );
		for (final Record record : sample_records) {
			System.out.println( Arrays.toString( record.getTokensArray() ) );
		}
		System.out.println(  );
		
		System.out.println( "rules: "+sample_rules.size() );
		for (final Rule rule : sample_rules) {
			System.out.println( rule.toString());
		}
		System.out.println(  );
		
		System.out.println( "pos qgrams: ");
		for (int i=0; i<sample_pos_qgrams.size(); i++) {
			for (final QGram qgram : sample_pos_qgrams.get(i)) {
				System.out.println( "["+qgram.toString()+", "+i+"]" );
			}
		}
	}

	public static void main( String[] args ) throws IOException {
		
		/* preprocessing */
		//"D:\\ghsong\\data\\yjpark_data\\data1_1000000_5_10000_1.0_0.0_1.txt";
		//"D:\\ghsong\\data\\yjpark_data\\data2_1000000_5_15848_1.0_0.0_2.txt";
		//"D:\\ghsong\\data\\yjpark_data\\rule1_30000_2_2_10000_0.0_0.txt";
		//"D:\\ghsong\\data\\yjpark_data\\rule2_30000_2_2_30000_0.0_0.txt";


		final String dataOnePath = "D:\\ghsong\\data\\yjpark_data\\data2_1000000_5_15848_1.0_0.0_2.txt";
		final String dataTwoPath = "D:\\ghsong\\data\\yjpark_data\\data2_1000000_5_15848_1.0_0.0_2.txt";
		final String rulePath = "D:\\ghsong\\data\\yjpark_data\\rule2_30000_2_2_30000_0.0_0.txt";
		final String outputPath = "output";
		final Boolean oneSideJoin = true;
		final Query query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		final int q = 2;
		
		//[24397 3252 10978 5663]
  	
		/* DEBUG: additional data */
		Rule rule0 = new Rule( new int[] {24397, 3252, 10978}, new int[] {777} );
		//System.out.println( rule0.toOriginalString( query.tokenIndex ) );
//		query.ruleSet.add(rule0);

		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		//System.exit(1);
		
		
		/* sample some records and test the DP algorithm. */
//		final int[] idxArray = {2, 5, 8077, 11165, 12444};
		final int[] idxArray = {};
		final IntOpenHashSet idxSet = new IntOpenHashSet(idxArray);
		for (int i=0; i<2000; i++) idxSet.add( i );
		final int pos_max = 30;
		ObjectArrayList<Record> sample_records = new ObjectArrayList<Record>();
		ObjectOpenHashSet<Rule> sample_rules = new ObjectOpenHashSet<Rule>();
		ObjectArrayList<ObjectOpenHashSet<QGram>> sample_pos_qgrams = new ObjectArrayList<ObjectOpenHashSet<QGram>>();
		for (int i=0; i<pos_max; i++) sample_pos_qgrams.add( new ObjectOpenHashSet<QGram>() );

		int idx = 0;
		for ( final Record record : query.indexedSet.get()) {
			if (idxSet.contains( idx )) {
				record.preprocessRules(automata);
				record.preprocessSuffixApplicableRules();
				record.preprocessTransformLength();
				if (debug) System.out.println("idx: "+idx);
				if (debug) System.out.println("record: "+Arrays.toString( record.getTokensArray() ));
				sample_records.add( record );

				if (debug) System.out.println( "applicable rules: " );
				for (int pos=0; pos<record.size(); pos++ ) {
					for (final Rule rule : record.getSuffixApplicableRules( pos )) {
						if (debug) System.out.println("\t("+rule.toString()+", "+pos+")");
						sample_rules.add( rule );
					}
				}

				if (debug) System.out.println( "transformed strings: " );
				final List<Record> expanded = record.expandAll();
				for( final Record exp : expanded ) {
					if (debug) System.out.println( "\t"+Arrays.toString( exp.getTokensArray() ) );
				}
				
				if (debug) System.out.println( "positional q-grams: " );
				List<List<QGram>> qgrams_self = record.getSelfQGrams( q, record.getTokenCount() );
				for (int i=0; i<qgrams_self.size(); i++) {
					for (final QGram qgram : qgrams_self.get(i)) {
						if (debug) System.out.println( "\t["+qgram.toString()+", "+i+"]" );
					}
				}
				
				if (debug) System.out.println( "positional q-grams in a transformed string: " );
				List<List<QGram>> qgrams = record.getQGrams(q);
				for (int i=0; i<qgrams.size(); i++) {
					for (final QGram qgram : qgrams.get(i)) {
						if (debug) System.out.println( "\t["+qgram.toString()+", "+i+"]" );
						sample_pos_qgrams.get( i ).add( qgram );
					}
				}
				
			} // end if idxSet.contains

			idx++;
			if (idx > 20000) break;
		}
		
		
		/* check the retrieved data */
//		check_samples( query, sample_records, sample_rules, sample_pos_qgrams );
		
		System.out.println( "Number of records: "+sample_records.size() );
		System.out.println( "Number of rules: "+sample_rules.size() );
		int n_pos_qgrams = 0;
		for (int i=0; i<sample_pos_qgrams.size(); i++) n_pos_qgrams += sample_pos_qgrams.get( i ).size();
		System.out.println( "Number of pos q-grams: "+n_pos_qgrams);
		
		int ridx = 0;
		StopWatch totalTime = StopWatch.getWatchStarted( "total time" );
		for (final Record record : sample_records) {
			ridx++;
//			if (ridx != 334) continue;
			System.out.println( "record "+ridx+": "+Arrays.toString( record.getTokensArray()) );
//		final Record record = sample_records.get( 4 );
//			inspect_record( record, query, q );
			PosQGramFilterDP posQGramFilterDP = new PosQGramFilterDP(record, q);
//			posQGramFilterDP.testIsSubstringOf();
//			posQGramFilterDP.testIsPrefixOf();
			for (int pos=0; pos<sample_pos_qgrams.size(); pos++ ) {
				ObjectOpenHashSet<QGram> set_tpq;
				if (pos < record.getQGrams( q ).size()) set_tpq = new ObjectOpenHashSet<>( record.getQGrams(q).get( pos ) );
				else set_tpq = new ObjectOpenHashSet<>();
				ObjectOpenHashSet<QGram> set_tpq_DP = new ObjectOpenHashSet<>();
				for (final QGram qgram : sample_pos_qgrams.get( pos )) {
//					System.out.println( "*******************************************" );
//					System.out.println( "pos qgram: ["+Arrays.toString( qgram.qgram )+", "+pos+"]");
					Boolean isInTPQ = posQGramFilterDP.existence( qgram, pos );
//					if (isInTPQ) System.out.println( "["+Arrays.toString( qgram.qgram )+", "+pos+"]\t"+isInTPQ );
					if (isInTPQ) set_tpq_DP.add( qgram );
					if (isInTPQ && !set_tpq.contains( qgram ) || !isInTPQ && set_tpq.contains( qgram )) {
						inspect_record( record, query, q );
						System.err.println( "record "+ridx );
						System.err.println( set_tpq.toString() );
						System.err.println( qgram+", "+isInTPQ );
						throw new AssertionError();
					}
				}
			}
		}
		totalTime.stop();
	} // end main
}
