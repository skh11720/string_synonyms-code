package tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;

public class Rule_ACAutomata {
	private class State {
		State func;
		ArrayList<Rule> output;
		State parent;
		IntegerMap<State> split;
		int token;

		State() {
			func = this;
		}

		State( int token, State parent ) {
			this.token = token;
			this.parent = parent;
		}
	}

	private final State root;

	public Rule_ACAutomata( Iterable<Rule> rules ) {
		// 1. Create a root state
		root = new State();

		// 2. Build Trie for rules
		for( final Rule rule : rules ) {
			State curr = root;
			for( final int str : rule.from ) {
				State next;
				if( curr.split != null && ( next = curr.split.get( str ) ) != null ) {
					curr = next;
				}
				else {
					next = new State( str, curr );
					if( curr.split == null ) {
						curr.split = new IntegerMap<>();
					}
					curr.split.put( str, next );
					curr = next;
				}
			}
			if( curr.output == null ) {
				curr.output = new ArrayList<>( 3 );
			}
			curr.output.add( rule );
		}

		// 3. Calculate the failure function
		// Use BFS
		ArrayList<State> currdepth = new ArrayList<>();
		ArrayList<State> nextdepth = new ArrayList<>();

		// Calculate depth-1 states
		for( final Entry<Integer, State> depth_1_entries : root.split.entrySet() ) {
			final State state = depth_1_entries.getValue();
			state.func = root;
			// Add depth-2 states
			if( state.split != null ) {
				for( final Entry<Integer, State> depth_2_entries : state.split.entrySet() ) {
					currdepth.add( depth_2_entries.getValue() );
				}
			}
		}

		// Calculate depth-x states
		while( !currdepth.isEmpty() ) {
			for( final State curr : currdepth ) {
				State r = curr.parent.func;
				while( true ) {
					if( r == root || r.split != null && r.split.containsKey( curr.token ) ) {
						break;
					}
					else {
						r = r.func;
					}
				}
				if( r.split.containsKey( curr.token ) ) {
					curr.func = r.split.get( curr.token );
				}
				else {
					curr.func = root;
				}

				// Compute output function
				if( curr.func.output != null ) {
					if( curr.output == null ) {
						curr.output = new ArrayList<>();
					}
					curr.output.addAll( curr.func.output );
				}

				// Add next states
				if( curr.split != null ) {
					for( final Entry<Integer, State> child : curr.split.entrySet() ) {
						nextdepth.add( child.getValue() );
					}
				}
			}

			final ArrayList<State> tmp = currdepth;
			currdepth = nextdepth;
			nextdepth = tmp;
			nextdepth.clear();
		}
	}

	public Rule[] applicableRules( int[] tokens ) {
		final HashSet<Rule> rslt = new HashSet<>();
		State curr = root;
		int i = 0;
		while( i < tokens.length ) {
			State next;
			if( curr.split != null && ( next = curr.split.get( tokens[ i ] ) ) != null ) {
				curr = next;
				++i;
				if( next.output != null ) {
					for( final Rule rule : next.output ) {
						rslt.add( rule );
					}
				}
			}
			else if( curr == root ) {
				++i;
			}
			else {
				curr = curr.func;
			}
		}
		return rslt.toArray( new Rule[ 0 ] );
	}

	/**
	 * Do not Automatically adds a self rule
	 */
	@SuppressWarnings( "unchecked" )
	public Rule[][] applicableRules( int[] tokens, int startIdx ) {
		final HashSet<Rule>[] tmprslt = new HashSet[ tokens.length ];
		for( int i = 0; i < tokens.length; ++i ) {
			tmprslt[ i ] = new HashSet<>();
		}
		for( int i = 0; i < startIdx; ++i ) {
			final int t = tokens[ i ];
			tmprslt[ i ].add( new Rule( t, t ) );
		}
		State curr = root;
		int i = startIdx;
		while( i < tokens.length ) {
			State next;
			if( curr.split != null && ( next = curr.split.get( tokens[ i ] ) ) != null ) {
				curr = next;
				++i;
				if( next.output != null ) {
					for( final Rule rule : next.output ) {
						tmprslt[ i - rule.getFrom().length ].add( rule );
					}
				}
			}
			else if( curr == root ) {
				++i;
			}
			else {
				curr = curr.func;
			}
		}

		final Rule[][] result = new Rule[ tokens.length ][];
		for( i = 0; i < tokens.length; ++i ) {
			result[ i ] = tmprslt[ i ].toArray( new Rule[ 0 ] );
		}
		return result;
	}
}
