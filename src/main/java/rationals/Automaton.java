package rationals;

import java.util.*;

import rationals.converters.toAscii;
import rationals.transformations.TransformationsToolBox;

public class Automaton
implements Acceptor,
StateMachine,
Rational,
Cloneable {
    protected final Set<Object> alphabet;
    private final Set<State> states;
    private final Set<State> initials;
    private final Set<State> terminals;
    private final Map<Key, Set<Transition>> transitions;
    private final Map<Key, Set<Transition>> reverse;
    private final StateFactory stateFactory;
    private final Map<Object, State> labels = new HashMap<Object, State>();

    @Override
    public StateFactory getStateFactory() {
        return this.stateFactory;
    }

    public static Automaton epsilonAutomaton() {
        Automaton v = new Automaton();
        v.addState(true, true);
        return v;
    }

    public static Automaton labelAutomaton(Object label) {
        Automaton v = new Automaton();
        State start = v.addState(true, false);
        State end = v.addState(false, true);
        v.addTransition(new Transition(start, label, end), null);
        return v;
    }

    public Automaton() {
        this(null);
    }

    public Automaton(StateFactory sf) {
        this.stateFactory = sf == null ? new DefaultStateFactory(this) : sf;
        this.alphabet = new HashSet<Object>();
        this.states = this.stateFactory.stateSet();
        this.initials = this.stateFactory.stateSet();
        this.terminals = this.stateFactory.stateSet();
        this.transitions = new HashMap<Key, Set<Transition>>();
        this.reverse = new HashMap<Key, Set<Transition>>();
    }

    @Override
    public State addState(boolean initial, boolean terminal) {
        State state = this.stateFactory.create(initial, terminal);
        if (initial) {
            this.initials.add(state);
        }
        if (terminal) {
            this.terminals.add(state);
        }
        this.states.add(state);
        return state;
    }

    @Override
    public Set<Object> alphabet() {
        return this.alphabet;
    }

    @Override
    public Set<State> states() {
        return this.states;
    }

    @Override
    public Set<State> initials() {
        return this.initials;
    }

    @Override
    public Set<State> terminals() {
        return this.terminals;
    }

    protected Set<State> access(Set<State> start, Map<Key, Set<Transition>> map) {
        Set<State> old;
        Set<State> current = start;
        do {
            old = current;
            current = this.stateFactory.stateSet();
            for (State e : old) {
                current.add(e);
                Iterator<Object> j = this.alphabet.iterator();
                while (j.hasNext()) {
                    Iterator<Transition> k = this.find(map, e, j.next()).iterator();
                    while (k.hasNext()) {
                        current.add(k.next().end());
                    }
                }
            }
        } while (current.size() != old.size());
        return current;
    }

    @Override
    public Set<State> accessibleStates() {
        return this.access(this.initials, this.transitions);
    }

    @Override
    public Set<State> coAccessibleStates() {
        return this.access(this.terminals, this.reverse);
    }

    @Override
    public Set<State> accessibleAndCoAccessibleStates() {
        Set<State> ac = this.accessibleStates();
        ac.retainAll(this.coAccessibleStates());
        return ac;
    }

    protected Set<Transition> find(Map<Key, Set<Transition>> m, State e, Object l) {
        Key n = new Key(e, l);
        if (!m.containsKey(n)) {
            return new HashSet<Transition>();
        }
        return m.get(n);
    }

    protected void add(Map<Key, Set<Transition>> m, Transition t) {
        Set<Transition> s;
        Key n = new Key(t.start(), t.label());
        if (!m.containsKey(n)) {
            s = new HashSet();
            m.put(n, s);
        } else {
            s = m.get(n);
        }
        s.add(t);
    }

    @Override
    public Set<Transition> delta() {
        HashSet<Transition> s = new HashSet<Transition>();
        for (Set<Transition> tr : this.transitions.values()) {
            s.addAll(tr);
        }
        return s;
    }

    @Override
    public Set<Transition> delta(State state, Object label) {
        return this.find(this.transitions, state, label);
    }

    @Override
    public Set<Transition> deltaFrom(State from, State to) {
        Set<Transition> t = this.delta(from);
        Iterator<Transition> i = t.iterator();
        while (i.hasNext()) {
            Transition tr = i.next();
            if (to.equals(tr.end())) continue;
            i.remove();
        }
        return t;
    }

    @Override
    public Set<Transition> delta(State state) {
        HashSet<Transition> s = new HashSet<Transition>();
        for (Object lt : this.alphabet) {
            s.addAll(this.delta(state, lt));
        }
        return s;
    }

    @Override
    public Set<Transition> delta(Set<State> s) {
        HashSet<Transition> ds = new HashSet<Transition>();
        for (State st : s) {
            ds.addAll(this.delta(st));
        }
        return ds;
    }

    @Override
    public Set<Transition> deltaMinusOne(State state, Object label) {
        return this.find(this.reverse, state, label);
    }

    @Override
    public boolean addTransition(Transition transition) {
        this.alphabet.add(transition.label());
        this.add(this.transitions, transition);
        this.add(this.reverse, new Transition(transition.end(), transition.label(), transition.start()));
        return true;
    }

    @Override
    public boolean validTransition(Transition transition) {
        return transition == null || this.states.contains(transition.start()) && this.states.contains(transition.end());
    }

    @Override
    public boolean addTransition(Transition transition, String ifInvalid) {
        if (this.validTransition(transition)) {
            return this.addTransition(transition);
        }
        if (ifInvalid != null) {
            throw new IllegalArgumentException(ifInvalid);
        }
        return false;
    }

    public String toString() {
        return new toAscii().toString(this);
    }

    public Object clone() {
        Automaton b = new Automaton();
        HashMap<State, State> map = new HashMap<State, State>();
        for (State e : this.states) {
            map.put(e, b.addState(e.isInitial(), e.isTerminal()));
        }
        for (Transition t : this.delta()) {
            b.addTransition(new Transition(map.get(t.start()), t.label(), map.get(t.end())), null);
        }
        return b;
    }

    @Override
    public Set<State> steps(List<?> word) {
        Set<State> s = TransformationsToolBox.epsilonClosure(this.initials(), this);
        return this.steps(s, word);
    }

    @Override
    public Set<State> steps(Set<State> s, List<?> word) {
        for (Object o : word) {
            if (!(s = this.step(s, o)).isEmpty()) continue;
            return s;
        }
        return s;
    }

    @Override
    public Set<State> step(Set<State> s, Object o) {
        Set<State> ns = this.stateFactory.stateSet();
        Set<State> ec = TransformationsToolBox.epsilonClosure(s, this);
        for (State st : ec) {
            for (Transition tr : this.delta(st)) {
                if (tr.label() == null || !tr.label().equals(o)) continue;
                ns.add(tr.end());
            }
        }
        return ns;
    }

    @Override
    public Set<Transition> deltaMinusOne(State st) {
        HashSet<Transition> s = new HashSet<Transition>();
        Iterator<Object> alphit = this.alphabet().iterator();
        while (alphit.hasNext()) {
            s.addAll(this.deltaMinusOne(st, alphit.next()));
        }
        return s;
    }

    public State state(Object label) {
        State s = this.labels.get(label);
        if (s == null) {
            s = this.stateFactory.create(false, false);
            this.states.add(s);
            this.labels.put(label, s);
        }
        return s;
    }

    private class Key {
        State s;
        Object l;

        protected Key(State s, Object l) {
            this.s = s;
            this.l = l;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null || !(o instanceof Key)) {
                return false;
            }
            Key t = (Key)o;
            return (Objects.equals(this.l, t.l)) && (Objects.equals(this.s, t.s));
        }

        public int hashCode() {
            int x = this.s == null ? 0 : this.s.hashCode();
            int y = this.l == null ? 0 : this.l.hashCode();
            return y << 16 | x;
        }
    }

}

