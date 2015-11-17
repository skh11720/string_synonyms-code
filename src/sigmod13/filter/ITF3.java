package sigmod13.filter;

import java.util.ArrayList;

import sigmod13.SIRecord;
import tools.IntegerMap;
import tools.Rule;

public class ITF3 extends ITF_Filter {
  private IntegerMap<Long> tfs;

  public ITF3(ArrayList<SIRecord> records, ArrayList<Rule> rules) {
    super(records, rules);
    tfs = new IntegerMap<Long>();

    for (SIRecord rec : records) {
      for (int token : rec.getTokens()) {
        Long freq = tfs.get(token);
        if (freq == null)
          freq = 1L;
        else
          freq = freq + 1;
        tfs.put(token, freq);
      }
    }

    for (Rule rule : rules) {
      for (int token : rule.getFrom()) {
        Long freq = tfs.get(token);
        if (freq == null)
          freq = 1L;
        else
          freq = freq + 1;
        tfs.put(token, freq);
      }

      for (int token : rule.getTo()) {
        Long freq = tfs.get(token);
        if (freq == null)
          freq = 1L;
        else
          freq = freq + 1;
        tfs.put(token, freq);
      }
    }
  }

  @Override
  /*
   * Precedence priority:
   * 1) Less ITF value (the ITF value is integrated)
   */
  public int compare(int t1, boolean t1_from_record, int t2,
      boolean t2_from_record) {
    Long f1 = tfs.get(t1);
    Long f2 = tfs.get(t2);
    // null equals to 0
    if (f1 == null) {
      if (f2 == null) return 0;
      return -1;
    } else if (f2 == null) return 1;
    return Long.compare(f1, f2);
  }
}
