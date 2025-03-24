package rationals.transformations;

import java.util.HashMap;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;
import rationals.properties.ContainsEpsilon;

public class Concatenation
implements BinaryTransformation {
    @Override
    public Automaton transform(Automaton a, Automaton b) {
        State n;
        Automaton ap = new Normalizer().transform(a);
        Automaton bp = new Normalizer().transform(b);
        ContainsEpsilon ce = new ContainsEpsilon();
        boolean ace = ce.test(a);
        boolean bce = ce.test(b);
        if (ap.states().size() == 0 && ace) {
            return b;
        }
        if (bp.states().size() == 0 && bce) {
            return a;
        }
        State junc = null;
        Automaton c = new Automaton();
        HashMap<State, State> map = new HashMap<State, State>();
        for (State e : ap.states()) {
            if (e.isInitial()) {
                n = c.addState(true, ace && bce);
            } else {
                if (e.isTerminal()) continue;
                n = c.addState(false, e.isTerminal() && bce);
            }
            map.put(e, n);
        }
        for (State e : bp.states()) {
            if (e.isInitial()) continue;
            n = c.addState(false, e.isTerminal());
            map.put(e, n);
        }
        junc = c.addState(ace, bce);
        for (Transition t : ap.delta()) {
            if (t.end().isTerminal()) {
                c.addTransition(new Transition(map.get(t.start()), t.label(), junc), null);
                continue;
            }
            c.addTransition(new Transition(map.get(t.start()), t.label(), map.get(t.end())), null);
        }
        for (Transition t : bp.delta()) {
            if (t.start().isInitial()) {
                c.addTransition(new Transition(junc, t.label(), map.get(t.end())), null);
                continue;
            }
            c.addTransition(new Transition(map.get(t.start()), t.label(), map.get(t.end())), null);
        }
        return c;
    }
}

