package rationals;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class DefaultStateFactory
implements StateFactory,
Cloneable {
    protected int id = 0;
    Automaton automaton;

    DefaultStateFactory(Automaton a) {
        this.automaton = a;
    }

    @Override
    public State create(boolean initial, boolean terminal) {
        return new DefaultState(this.id++, initial, terminal);
    }

    @Override
    public Set<State> stateSet() {
        return new DefaultStateSet(this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DefaultStateFactory cl = (DefaultStateFactory)super.clone();
        cl.id = 0;
        return cl;
    }

    class DefaultStateSet
    implements Set<State> {
        private final DefaultStateFactory df;
        int modcount = 0;
        int mods = 0;
        int bit = -1;
        final BitSet bits = new BitSet();
        final Iterator<State> it = new Iterator<State>(){

            @Override
            public void remove() {
                if (DefaultStateSet.this.bit > 0) {
                    DefaultStateSet.this.bits.clear(DefaultStateSet.this.bit);
                }
            }

            @Override
            public boolean hasNext() {
                return DefaultStateSet.this.bits.nextSetBit(DefaultStateSet.this.bit) > -1;
            }

            @Override
            public State next() {
                DefaultStateSet.this.bit = DefaultStateSet.this.bits.nextSetBit(DefaultStateSet.this.bit);
                if (DefaultStateSet.this.bit == -1) {
                    throw new NoSuchElementException();
                }
                DefaultState ds = new DefaultState(DefaultStateSet.this.bit, false, false);
                ds.initial = DefaultStateFactory.this.automaton.initials().contains(ds);
                ds.terminal = DefaultStateFactory.this.automaton.terminals().contains(ds);
                ++DefaultStateSet.this.mods;
                ++DefaultStateSet.this.modcount;
                if (DefaultStateSet.this.mods != DefaultStateSet.this.modcount) {
                    throw new ConcurrentModificationException();
                }
                ++DefaultStateSet.this.bit;
                return ds;
            }
        };

        public DefaultStateSet(DefaultStateFactory df) {
            this.df = df;
        }

        @Override
        public boolean equals(Object obj) {
            DefaultStateSet dss = (DefaultStateSet)obj;
            return dss != null && dss.bits.equals(this.bits) && dss.df == this.df;
        }

        @Override
        public int hashCode() {
            return this.bits.hashCode();
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append('[');
            String b = this.bits.toString();
            sb.append(b, 1, b.length() - 1);
            sb.append(']');
            return sb.toString();
        }

        @Override
        public int size() {
            return this.bits.cardinality();
        }

        @Override
        public void clear() {
            ++this.modcount;
            this.bits.clear();
        }

        @Override
        public boolean isEmpty() {
            return this.bits.isEmpty();
        }

        @Override
        public Object[] toArray() {
            Object[] ret = new Object[this.size()];
            Iterator<State> l = this.iterator();
            int i = 0;
            while (l.hasNext()) {
                ret[i++] = l.next();
            }
            return ret;
        }

        @Override
        public boolean add(State o) {
            DefaultState ds = (DefaultState)o;
            if (this.bits.get(ds.i)) {
                return false;
            }
            this.bits.set(ds.i);
            ++this.modcount;
            return true;
        }

        @Override
        public boolean contains(Object o) {
            DefaultState ds = (DefaultState)o;
            return this.bits.get(ds.i);
        }

        @Override
        public boolean remove(Object o) {
            DefaultState ds = (DefaultState)o;
            if (!this.bits.get(ds.i)) {
                return false;
            }
            this.bits.clear(ds.i);
            ++this.modcount;
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends State> c) {
            DefaultStateSet dss = (DefaultStateSet)c;
            this.bits.or(dss.bits);
            ++this.modcount;
            return true;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            DefaultStateSet dss = (DefaultStateSet)c;
            BitSet bs = new BitSet();
            bs.or(this.bits);
            bs.and(dss.bits);
            ++this.modcount;
            return bs.equals(dss.bits);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            DefaultStateSet dss = (DefaultStateSet)c;
            this.bits.andNot(dss.bits);
            ++this.modcount;
            return true;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            DefaultStateSet dss = (DefaultStateSet)c;
            this.bits.and(dss.bits);
            ++this.modcount;
            return true;
        }

        @Override
        public Iterator<State> iterator() {
            this.mods = 0;
            this.modcount = 0;
            this.bit = 0;
            return this.it;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            Iterator<State> iterator = this.iterator();
            ArrayList<State> l = new ArrayList<State>();
            while (iterator.hasNext()) {
                l.add(iterator.next());
            }
            return l.toArray(a);
        }

    }

    class DefaultState
    implements State {
        public final int i;
        boolean initial;
        boolean terminal;
        final Automaton a;

        DefaultState(int i, boolean initial, boolean terminal) {
            this.i = i;
            this.a = DefaultStateFactory.this.automaton;
            this.initial = initial;
            this.terminal = terminal;
        }

        @Override
        public boolean isInitial() {
            return this.initial;
        }

        @Override
        public boolean isTerminal() {
            return this.terminal;
        }

        public String toString() {
            return Integer.toString(this.i);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DefaultState)) {
                return false;
            }
            DefaultState ds = (DefaultState)o;
            return ds.i == this.i && this.a == ds.a;
        }

        public int hashCode() {
            return this.i;
        }
    }

}

