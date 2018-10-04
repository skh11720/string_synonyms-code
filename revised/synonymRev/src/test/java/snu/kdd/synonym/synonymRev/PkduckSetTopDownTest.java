package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.FrequencyFirstOrder;
import vldb17.set.PkduckSetDP;
import vldb17.set.PkduckSetDPTopDown;

public class PkduckSetTopDownTest {
	String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K"};
	int[] sizeList = {10000, 100000};
	
	@Test
	public void testCorrectness() throws IOException {
		for ( int size : sizeList ) {
			for ( String dataset : datasetList ) {
				System.out.println( dataset+'\t'+size );
				Query query = TestUtils.getTestQuery(dataset, size);

				FrequencyFirstOrder globalOrder = new FrequencyFirstOrder( 1 );
				globalOrder.initializeForSet( query );
				long tsum0 = 0, tsum1 = 0;
				
				for ( Record recS : query.searchedSet.recordList ) {
					IntOpenHashSet candTokenSet = new IntOpenHashSet();
					for ( Rule[] rules : recS.getApplicableRules() ) {
						for ( Rule r : rules ) {
							for ( int token : r.getRight() ) candTokenSet.add( token );
						}
					}
					
					PkduckSetDP pkduck0 = new PkduckSetDP( recS, globalOrder );
					PkduckSetDPTopDown pkduck1 = new PkduckSetDPTopDown( recS, globalOrder );
					for ( int token : candTokenSet ) {
						long ts = System.nanoTime();
						boolean b0 = pkduck0.isInSigU( token );
						long t0 = System.nanoTime();
						boolean b1 = pkduck1.isInSigU( token );
						long t1 = System.nanoTime();
						assertEquals( b0, b1 );
						tsum0 += (t0 - ts);
						tsum1 += (t1 - t0);
					}
				}
				System.out.println( dataset+'\t'+size+'\t'+(tsum0/1e6)+'\t'+(tsum1/1e6) );
//				break; // DEBUG
			}
//			break; // DEBUG
		}
	}
}
