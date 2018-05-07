package snu.kdd.synonym.synonymRev.tools;

import java.util.Comparator;

public class QGramComparator implements Comparator<QGram> {

	@Override
	public int compare( QGram o1, QGram o2 ) {
		int[] qgram1 = o1.qgram;
		int[] qgram2 = o2.qgram;
		int len = Math.min( qgram1.length, qgram2.length );
		int res = Integer.MAX_VALUE;
		for (int i=0; i<len; i++) {
			res = Integer.compare( qgram1[i], qgram2[i] );
			if (res != 0) return res;
		}
		if (qgram1.length > len) return 1;
		else if (qgram2.length > len) return -1;
		else return 0;
	}
}
