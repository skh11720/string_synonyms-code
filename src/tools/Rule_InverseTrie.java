package tools;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

public class Rule_InverseTrie {
  private State root;

  public Rule_InverseTrie(Rule[][] rules) {
    // 1. Create a root state
    root = new State();

    // 2. Build Trie for rules
    for (int idx = 0; idx < rules.length; ++idx)
      for (Rule rule : rules[idx]) {
        State curr = root;
        for (int str : rule.to) {
          State next;
          if (curr.split != null && (next = curr.split.get(str)) != null)
            curr = next;
          else {
            next = new State();
            if (curr.split == null) curr.split = new IntegerMap<State>();
            curr.split.put(str, next);
            curr = next;
          }
        }
        if (curr.output == null) curr.output = new ArrayList<PositionalRule>(3);
        curr.output.add(new PositionalRule(rule, idx));
      }
  }

  public ArrayList<Rule> applicableRules(int[] residual, int idx) {
    State curr = root;
    ArrayList<Rule> rslt = new ArrayList<Rule>();
    int i = 0;
    while (i < residual.length) {
      int token = residual[i];
      State next;
      if(curr.output != null)
        for(PositionalRule prule : curr.output)
          if(prule.idx == idx) rslt.add(prule.rule);
      if (curr.split != null && (next = curr.split.get(token)) != null) {
        curr = next;
        ++i;
      } else
        return rslt;
    }
    Queue<State> queue = new LinkedList<State>();
    queue.add(curr);
    while (!queue.isEmpty()) {
      curr = queue.poll();
      if (curr.output != null) {
        for (PositionalRule prule : curr.output)
          if (prule.idx == idx) rslt.add(prule.rule);
      }
      if (curr.split != null)
        for (Entry<Integer, State> e : curr.split.entrySet()) {
        queue.add(e.getValue());
      }
    }
    return rslt;
  }

  private class State {
    IntegerMap<State>         split;
    ArrayList<PositionalRule> output;
  }

  private class PositionalRule {
    Rule rule;
    int  idx;

    PositionalRule(Rule rule, int idx) {
      this.rule = rule;
      this.idx = idx;
    }
  }
}
