package rationals.transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

public class Complement
implements UnaryTransformation {
    @Override
    public Automaton transform(Automaton a) {
        Automaton ret = new Automaton();
        ArrayList<State> todo = new ArrayList<State>();
        HashMap<State, State> sm = new HashMap<State, State>();
        HashSet<State> done = new HashSet<State>();
        Set<State> s = a.initials();
        todo.addAll(s);
        while (!todo.isEmpty()) {
            State st = todo.remove(0);
            State ns = sm.get(st);
            if (ns == null) {
                ns = ret.addState(st.isInitial(), !st.isTerminal());
                sm.put(st, ns);
            }
            done.add(st);
            for (Object l : a.alphabet()) {
                Set<Transition> ends = a.delta(st, l);
                if (ends.isEmpty()) {
                    ret.addTransition(new Transition(ns, l, ns), null);
                    continue;
                }
                Iterator<Transition> i = ends.iterator();
                while (i.hasNext()) {
                    State end = i.next().end();
                    State ne = sm.get(end);
                    if (ne == null) {
                        ne = ret.addState(end.isInitial(), !end.isTerminal());
                        sm.put(end, ne);
                        todo.add(end);
                    }
                    ret.addTransition(new Transition(ns, l, ne), null);
                }
            }
        }
        return ret;
    }
}

