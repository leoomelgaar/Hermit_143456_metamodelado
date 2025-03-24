package rationals.transformations;

import java.util.HashMap;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;
import rationals.properties.ContainsEpsilon;

public class Normalizer
implements UnaryTransformation {
    @Override
    public Automaton transform(Automaton a) {
        Automaton b = new Automaton();
        State ni = b.addState(true, false);
        State nt = b.addState(false, true);
        HashMap<State, State> map = new HashMap<State, State>();
        for (State st : a.states()) {
            map.put(st, b.addState(false, false));
        }
        if (new ContainsEpsilon().test(a)) {
            b.addTransition(new Transition(ni, null, nt), null);
        }
        for (Transition t : a.delta()) {
            if (t.start().isInitial() && t.end().isTerminal()) {
                b.addTransition(new Transition(ni, t.label(), nt), null);
            }
            if (t.start().isInitial()) {
                b.addTransition(new Transition(ni, t.label(), map.get(t.end())), null);
            }
            if (t.end().isTerminal()) {
                b.addTransition(new Transition(map.get(t.start()), t.label(), nt), null);
            }
            b.addTransition(new Transition(map.get(t.start()), t.label(), map.get(t.end())), null);
        }
        b = new Pruner().transform(b);
        return b;
    }
}

