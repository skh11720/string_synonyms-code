package sigmod13;

import java.util.Collection;
import java.util.Set;

import sigmod13.filter.ITF_Filter;
import snu.kdd.synonym.synonymRev.validator.Validator;

public interface RecordInterface {
	public int getID();

	public int getMinLength();

	public int getMaxLength();

	public int size();

	public Collection<Integer> getTokens();

	public double similarity( RecordInterface rec, Validator checker );

	public Set<Integer> getSignatures( ITF_Filter filter, double theta );

	public Set<? extends Expanded> generateAll();

	public interface Expanded {
		public RecordInterface toRecord();

		public int size();

		public double similarity( Expanded o, Validator checker );
	}
}
