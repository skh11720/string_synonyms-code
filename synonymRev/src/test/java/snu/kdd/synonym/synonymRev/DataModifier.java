package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

public class DataModifier {
	
	public static long seed = 0L;
	public static Random rand = new Random(seed);

	@Test
	public void modifyUSPS() throws IOException {
		/*
		 * For each record s in USPS, produce:
		 * - a record s' without zipcode
		 * - a record s'' equivalent to s' by applying some rules
		 * - a record s''' created by deleting from s'' a token chosen randomly 
		 * And output s', s'', s''' to the destination.
		 */
		final String osName = System.getProperty( "os.name" );
		String prefix = null;
		if ( osName.startsWith( "Windows" ) ) { prefix = "D:\\ghsong\\data\\synonyms\\"; }
		else if ( osName.startsWith( "Linux" ) ) { prefix = "run/data_store/"; }
		final int size = 1_000_000;
		final String sep = "\\" + File.separator;
		String dataOnePath, dataTwoPath, rulePath;
		dataOnePath = prefix + String.format( "JiahengLu"+sep+"splitted"+sep+"USPS_%d.txt", size );
		dataTwoPath = prefix + String.format( "JiahengLu"+sep+"splitted"+sep+"USPS_%d.txt", size );
		rulePath = prefix + "JiahengLu"+sep+"USPS_rule.txt";
		
		final Query query = new Query(rulePath, dataOnePath, dataTwoPath, true, "output");
		final ACAutomataR automata = new ACAutomataR(query.ruleSet.get());
		for ( final Record record : query.searchedSet.recordList ) {
			record.preprocessRules( automata );
		}
		
		ObjectArrayList<Record> recordList = new ObjectArrayList<>();
		String dstPath = prefix + String.format("USPS_mod1");
		File fDst = new File(dstPath);
		if ( !fDst.exists() ) fDst.mkdirs();
		int i=0;
		for ( final Record record : query.searchedSet.recordList ) {
			Record zipRemoved = getZIPRemoved(record, query);
			if ( zipRemoved != null ) {
				recordList.add(zipRemoved);
				Record equivRec = getEquivalent(zipRemoved, automata);
				
				if ( equivRec != null ) {
					recordList.add(equivRec);
					Record oneTokenRemoved = getOneTokenRemoved(equivRec);
					
					if ( oneTokenRemoved != null ) recordList.add(oneTokenRemoved);
				}
			}

//			System.out.println("ORIGINAL: " + record);
//			System.out.println("ORIGINAL INT ARRAY: " + Arrays.toString(record.getTokensArray()));
//			System.out.println("ZIP REMOVED: "+ zipRemoved.toString());
//			System.out.println("EQUIV RECORD: "+equivRec.toString());
//			System.out.println("TOKEN REMOVED: "+oneTokenRemoved.toString());
//			if ( ++i > 5 ) break;
			
		}

		Collections.shuffle(recordList, rand);
		PrintWriter pw = new PrintWriter( new BufferedWriter( new FileWriter(dstPath+sep+String.format("USPS_mod1_%d.txt", size))));
		for ( final Record record : recordList ) pw.println(record.toString());
		pw.close();
	}
	
	private static String[] getStringArray( final Record record, final Query query ) {
		final String[] tokens = new String[record.size()];
		for ( int i=0; i<record.size(); ++i ) tokens[i] = query.tokenIndex.getToken( record.getTokensArray()[i] );
		return tokens;
	}
	
	private static Record getZIPRemoved( final Record record, final Query query ) {
		String[] tokens = getStringArray(record, query);
		try { Integer.parseInt(tokens[tokens.length-1]); }
		catch ( NumberFormatException e ) { return null; }
		return new Record( Arrays.copyOfRange(record.getTokensArray(), 0, tokens.length-1));
	}
	
	private static Record getEquivalent( final Record record, final ACAutomataR automata ) {
		if ( record.applicableRules == null ) record.preprocessRules(automata);
		final int nRule = record.getNumApplicableRules();
		if ( nRule == 0 ) return null;
		final int[] tokenArr = record.getTokensArray();
		final IntArrayList tokenList = new IntArrayList();
		boolean transformed = false;
		while (!transformed) {
			tokenList.clear();
			for ( int k=0; k<record.size(); ++k ) {
				boolean transformedAtK = false;
				for ( final Rule rule : record.getApplicableRules(k) ) {
					if ( rule.isSelfRule() ) continue;
					if ( rand.nextBoolean() ) {
						k += rule.leftSize() - 1;
						for ( int token : rule.getRight() ) tokenList.add(token);
						transformed = transformedAtK = true;
						break;
					}
				}
				if (!transformedAtK) tokenList.add(tokenArr[k]);
			}
		}
		
		return new Record(tokenList.toIntArray());
	}
	
	private static Record getOneTokenRemoved( final Record record ) {
		if ( record.size() == 1 ) return null;
		final int idxRemoved = rand.nextInt(record.size());
		final int[] tokenArr = record.getTokensArray();
		final int[] newTokenArr = new int[record.size()-1];
		for ( int i=0; i<record.size(); ++i ) {
			if ( i < idxRemoved ) newTokenArr[i] = tokenArr[i];
			else if ( i > idxRemoved ) newTokenArr[i-1] = tokenArr[i];
		}
		return new Record(newTokenArr);
	}
}
