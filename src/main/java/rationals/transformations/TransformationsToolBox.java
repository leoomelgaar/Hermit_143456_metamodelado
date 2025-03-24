package rationals.transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

public class TransformationsToolBox {
    public static boolean containsATerminalState(Set<State> s) {
        for (State e : s) {
            if (!e.isTerminal()) continue;
            return true;
        }
        return false;
    }

    public static boolean containsAnInitialState(Set<State> s) {
        for (State e : s) {
            if (!e.isInitial()) continue;
            return true;
        }
        return false;
    }

    public static Set<State> epsilonClosure(Set<State> s, Automaton a) {
        Set<State> exp = a.getStateFactory().stateSet();
        exp.addAll(s);
        Set<State> view = a.getStateFactory().stateSet();
        Set<State> arr = a.getStateFactory().stateSet();
        arr.addAll(s);
        do {
            Set<State> ns = a.getStateFactory().stateSet();
            ns.addAll(exp);
            for (State st : ns) {
                for (Transition tr : a.delta(st)) {
                    if (tr.label() != null || view.contains(tr.end()) || tr.end().equals(st)) continue;
                    exp.add(tr.end());
                    arr.add(tr.end());
                }
                exp.remove(st);
                view.add(st);
            }
        } while (!exp.isEmpty());
        return arr;
    }

    public static Map<Object, Set<State>> mapAlphabet(Set<Transition> ts, Automaton a) {
        HashMap<Object, Set<State>> am = new HashMap<Object, Set<State>>();
        ArrayList<Transition> tas = new ArrayList<Transition>(ts);
        while (!tas.isEmpty()) {
            Transition tr = tas.remove(0);
            Object l = tr.label();
            if (l == null) continue;
            Set<State> as = am.get(l);
            if (as == null) {
                as = a.getStateFactory().stateSet();
                am.put(l, as);
            }
            as.add(tr.end());
        }
        return am;
    }
}

