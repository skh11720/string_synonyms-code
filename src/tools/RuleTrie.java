package tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

public class RuleTrie {
	private Node root;

	public RuleTrie(Iterable<Rule> rules) {
		root = new Node(-1);
		for (Rule rule : rules)
			root.addChild(0, rule);
	}

	public ArrayList<Rule> matchingRules(int[] tokens, int from, int to) {
		Node curr = root;
		for (int i = from; i < to; ++i) {
			if (curr.children == null)
				return null;
			curr = curr.children.get(tokens[i]);
			if (curr == null)
				return null;
		}
		return curr.rules;
	}

	public ArrayList<Rule> applicableRules(int[] tokens, int idx) {
		ArrayList<Rule> rslt = new ArrayList<Rule>();
		Node curr = root;
		while (idx < tokens.length) {
			Node next;
			if (curr.children != null
					&& (next = curr.children.get(tokens[idx])) != null) {
				curr = next;
				++idx;
				if (curr.rules != null)
					for (Rule rule : curr.rules)
						rslt.add(rule);
			} else
				break;
		}
		return rslt;
	}

	public void print() {
		root.print(0);
	}
}

class Node {
	int str;
	IntegerMap<Node> children = null;
	ArrayList<Rule> rules = null;

	Node(int str) {
		this.str = str;
	}

	void addChild(int startIdx, Rule rule) {
		if (startIdx == rule.from.length) {
			if (rules == null)
				rules = new ArrayList<Rule>();
			rules.add(rule);
			return;
		} else if (children == null)
			children = new IntegerMap<Node>();
		int currKey = rule.from[startIdx];
		if (!children.containsKey(currKey)) {
			Node newNode = new Node(currKey);
			children.put(currKey, newNode);
		}
		children.get(rule.from[startIdx]).addChild(startIdx + 1, rule);
	}

	void print(int depth) {
		String prefix = "";
		for (int i = 0; i < depth; ++i)
			prefix = prefix + "\t";
		System.out.println(prefix + str);
		if (rules != null) {
			System.out.print(prefix + "└" + "rules:");
			for (Rule rule : rules)
				System.out.print(" " + Arrays.toString(rule.to));
			System.out.println();
		}
		if (children != null) {
			for (Entry<Integer, Node> e : children.entrySet())
				e.getValue().print(depth + 1);
		}
	}
}
