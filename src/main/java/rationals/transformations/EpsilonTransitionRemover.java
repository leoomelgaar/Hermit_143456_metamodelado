package rationals.transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

public class EpsilonTransitionRemover
implements UnaryTransformation {
    @Override
    public Automaton transform(Automaton a) {
        Automaton ret = new Automaton();
        HashMap<HashValue, State> sm = new HashMap<HashValue, State>();
        HashSet<HashValue> done = new HashSet<HashValue>();
        ArrayList<HashValue> todo = new ArrayList<HashValue>();
        Set<State> cur = TransformationsToolBox.epsilonClosure(a.initials(), a);
        State is = ret.addState(true, TransformationsToolBox.containsATerminalState(cur));
        HashValue hv = new HashValue(cur);
        sm.put(hv, is);
        todo.add(hv);
        do {
            State ns;
            HashValue s;
            if ((ns = sm.get(s = todo.remove(0))) == null) {
                ns = ret.addState(false, TransformationsToolBox.containsATerminalState(s.s));
                sm.put(s, ns);
            }
            done.add(s);
            Map<Object, Set<State>> trm = EpsilonTransitionRemover.instructions(a.delta(s.s), a);
            for (Map.Entry<Object, Set<State>> e : trm.entrySet()) {
                Object o = e.getKey();
                Set<State> ar = e.getValue();
                hv = new HashValue(ar = TransformationsToolBox.epsilonClosure(ar, a));
                State ne = sm.get(hv);
                if (ne == null) {
                    ne = ret.addState(false, TransformationsToolBox.containsATerminalState(ar));
                    sm.put(hv, ne);
                }
                ret.addTransition(new Transition(ns, o, ne), null);
                if (done.contains(hv)) continue;
                todo.add(hv);
            }
        } while (!todo.isEmpty());
        return ret;
    }

    private static Map<Object, Set<State>> instructions(Set<Transition> s, Automaton a) {
        HashMap<Object, Set<State>> m = new HashMap<Object, Set<State>>();
        for (Transition tr : s) {
            Object l = tr.label();
            if (l == null) continue;
            Set<State> st = m.get(l);
            if (st == null) {
                st = a.getStateFactory().stateSet();
                m.put(l, st);
            }
            st.add(tr.end());
        }
        return m;
    }
}

