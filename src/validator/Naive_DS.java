package validator;

import java.util.Collections;
import java.util.List;

import mine.Record;

public class Naive_DS extends Validator {
	private final int threshold;
	private Record prevx;
	private List<Record> expandedx;
	private RecordComparator cmp = new RecordComparator();
	private Validator longvalidator = new BottomUpQueue_DS( true );

	public Naive_DS( int threshold ) {
		this.threshold = threshold;
	}

	public int isEqual( Record x, Record y ) {
		// If records have too many 1-expanded records, do not use this method
		if( x.getEstNumRecords() > threshold || y.getEstNumRecords() > threshold )
			return longvalidator.isEqual( x, y );
		// If there is no pre-expanded records, do expand
		if( prevx != x ) {
			expandedx = x.expandAll( Record.getRuleTrie() );
			Collections.sort( expandedx, cmp );
		}
		List<Record> expandedy = y.expandAll( Record.getRuleTrie() );
		Collections.sort( expandedy, cmp );
		int idxx = 0, idxy = 0;
		assert ( expandedx.size() > 0 );
		assert ( expandedy.size() > 0 );
		Record ex = expandedx.get( 0 );
		Record ey = expandedx.get( 0 );
		while( true ) {
			int c = cmp.compare( ex, ey );
			if( c == 0 )
				return 1;
			else if( c > 0 ) {
				if( ++idxy == expandedy.size() )
					break;
				ey = expandedy.get( idxy );
			}
			else {
				assert ( c < 0 );
				if( ++idxx == expandedx.size() )
					break;
				ex = expandedx.get( idxx );
			}
		}
		return -1;
	}
}
