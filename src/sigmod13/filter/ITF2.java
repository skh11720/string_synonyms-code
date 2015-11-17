package sigmod13.filter;

import java.util.ArrayList;

import sigmod13.SIRecord;
import tools.IntegerMap;
import tools.Rule;

public class ITF2 extends ITF_Filter {
  private IntegerMap<Long> tf_Record;
  private IntegerMap<Long> tf_Rule;

  public ITF2(ArrayList<SIRecord> records, ArrayList<Rule> rules) {
    super(records, rules);
    tf_Record = new IntegerMap<Long>();
    tf_Rule = new IntegerMap<Long>();

    for (SIRecord rec : records) {
      for (int token : rec.getTokens()) {
        Long freq = tf_Record.get(token);
        if (freq == null)
          freq = 1L;
        else
          freq = freq + 1;
        tf_Record.put(token, freq);
      }
    }

    for (Rule rule : rules) {
      for (int token : rule.getFrom()) {
        Long freq = tf_Rule.get(token);
        if (freq == null)
          freq = 1L;
        else
          freq = freq + 1;
        tf_Rule.put(token, freq);
      }

      for (int token : rule.getTo()) {
        Long freq = tf_Rule.get(token);
        if (freq == null)
          freq = 1L;
        else
          freq = freq + 1;
        tf_Rule.put(token, freq);
      }
    }
  }

  @Override
  /*
   * Precedence priority: 1) Tokens from the rule set 2) Less ITF value
   */
  public int compare(int t1, boolean t1_from_record, int t2,
      boolean t2_from_record) {
    if (t1_from_record == t2_from_record) {
      IntegerMap<Long> tfs = (t1_from_record) ? tf_Record : tf_Rule;
      Long f1 = tfs.get(t1);
      Long f2 = tfs.get(t2);
      // null equals to 0
      if (f1 == null) {
        if (f2 == null) return 0;
        return -1;
      } else if (f2 == null) return 1;
      return Long.compare(f1, f2);
    } else if (t1_from_record)
      return 1;
    else
      return -1;
  }
}
