package rationals.transformations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

public class Reducer
implements UnaryTransformation {
    private static boolean same(State e1, State e2, Automaton a, Map<State, Set<State>> m) {
        if (!m.get(e1).equals(m.get(e2))) {
            return false;
        }
        Set<Transition> tas = a.delta(e1);
        Set<Transition> tbs = a.delta(e2);
        for (Transition tr : tas) {
            State ep1 = tr.end();
            Set<Transition> tbsl = a.delta(e2, tr.label());
            if (tbsl.isEmpty()) {
                return false;
            }
            for (Transition tb : tbsl) {
                tbs.remove(tb);
                State ep2 = tb.end();
                if (m.get(ep1).equals(m.get(ep2))) continue;
                return false;
            }
        }
        return tbs.isEmpty();
    }

    @Override
    public Automaton transform(Automaton a) {
        HashMap<State, Set<State>> old;
        Automaton b = new ToDFA().transform(a);
        HashMap<State, Set<State>> current = new HashMap<State, Set<State>>();
        Set<State> s1 = b.getStateFactory().stateSet();
        Set<State> s2 = b.getStateFactory().stateSet();
        for (State e : b.states()) {
            if (e.isTerminal()) {
                s1.add(e);
                current.put(e, s1);
                continue;
            }
            s2.add(e);
            current.put(e, s2);
        }
        do {
            old = current;
            current = new HashMap();
            for (State e1 : old.keySet()) {
                Set s = b.getStateFactory().stateSet();
                for (State e2 : current.keySet()) {
                    if (!Reducer.same(e1, e2, b, old)) continue;
                    s = current.get(e2);
                    break;
                }
                s.add(e1);
                current.put(e1, s);
            }
        } while (!new HashSet(current.values()).equals(new HashSet(old.values())));
        Automaton c = new Automaton();
        HashSet<Set> setSet = new HashSet<Set>(current.values());
        Iterator sets = setSet.iterator();
        HashMap<Set, State> newStates = new HashMap<Set, State>();
        while (sets.hasNext()) {
            Set set = (Set)sets.next();
            boolean term = TransformationsToolBox.containsATerminalState(set);
            boolean init = TransformationsToolBox.containsAnInitialState(set);
            newStates.put(set, c.addState(init, term));
        }
        for (Set set : setSet) {
            State r = (State)set.iterator().next();
            State rp = newStates.get(set);
            for (Object l : b.alphabet()) {
                Set<Transition> ds = b.delta(r, l);
                if (ds.isEmpty()) continue;
                State f = ds.iterator().next().end();
                State fp = newStates.get(current.get(f));
                c.addTransition(new Transition(rp, l, fp), null);
            }
        }
        return c;
    }
}

