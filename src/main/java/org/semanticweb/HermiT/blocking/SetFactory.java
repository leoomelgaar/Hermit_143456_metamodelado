package org.semanticweb.HermiT.blocking;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class SetFactory<E>
implements Serializable {
    private static final long serialVersionUID = 7071071962187693657L;
    protected Entry[] m_unusedEntries = new Entry[32];
    protected Entry[] m_entries = new Entry[16];
    protected int m_size = 0;
    protected int m_resizeThreshold = 12;

    public void clearNonpermanent() {
        for (int i = this.m_entries.length - 1; i >= 0; --i) {
            Entry entry = this.m_entries[i];
            while (entry != null) {
                Entry nextEntry = entry.m_nextEntry;
                if (!entry.m_permanent) {
                    this.removeEntry(entry);
                    this.leaveEntry(entry);
                }
                entry = nextEntry;
            }
        }
    }

    public void addReference(Set<E> set) {
        ++((Entry)set).m_referenceCount;
    }

    public void removeReference(Set<E> set) {
        Entry entry = (Entry)set;
        --entry.m_referenceCount;
        if (entry.m_referenceCount == 0 && !entry.m_permanent) {
            this.removeEntry(entry);
            this.leaveEntry(entry);
        }
    }

    public void makePermanent(Set<E> set) {
        ((Entry)set).m_permanent = true;
    }

    public Set<E> getSet(Set<E> elements) {
        int hashCode = elements.hashCode();
        int index = SetFactory.getIndexFor(hashCode, this.m_entries.length);
        Entry<E> entry = this.m_entries[index];
        while (entry != null) {
            if (hashCode == entry.m_hashCode && entry.equalsTo(elements)) {
                return entry;
            }
            entry = entry.m_nextEntry;
        }
        entry = this.getEntry(elements.size());
        entry.initialize(elements, hashCode);
        entry.m_previousEntry = null;
        entry.m_nextEntry = this.m_entries[index];
        if (entry.m_nextEntry != null) {
            entry.m_nextEntry.m_previousEntry = entry;
        }
        this.m_entries[index] = entry;
        ++this.m_size;
        if (this.m_size > this.m_resizeThreshold) {
            this.resize();
        }
        return entry;
    }

    protected void resize() {
        Entry[] newEntries = new Entry[this.m_entries.length * 2];
        for (int index = 0; index < this.m_entries.length; ++index) {
            Entry entry = this.m_entries[index];
            while (entry != null) {
                Entry nextEntry = entry.m_nextEntry;
                int newIndex = SetFactory.getIndexFor(entry.m_hashCode, newEntries.length);
                entry.m_nextEntry = newEntries[newIndex];
                entry.m_previousEntry = null;
                if (entry.m_nextEntry != null) {
                    entry.m_nextEntry.m_previousEntry = entry;
                }
                newEntries[newIndex] = entry;
                entry = nextEntry;
            }
        }
        this.m_entries = newEntries;
        this.m_resizeThreshold = (int)(0.75 * (double)this.m_entries.length);
    }

    protected void removeEntry(Entry<E> entry) {
        int index;
        if (entry.m_nextEntry != null) {
            entry.m_nextEntry.m_previousEntry = entry.m_previousEntry;
        }
        if (entry.m_previousEntry != null) {
            entry.m_previousEntry.m_nextEntry = entry.m_nextEntry;
        }
        if (this.m_entries[index = SetFactory.getIndexFor(entry.m_hashCode, this.m_entries.length)] == entry) {
            this.m_entries[index] = entry.m_nextEntry;
        }
        entry.m_nextEntry = null;
        entry.m_previousEntry = null;
    }

    protected Entry<E> getEntry(int size) {
        Entry entry;
        if (size >= this.m_unusedEntries.length) {
            int newSize = this.m_unusedEntries.length;
            while (newSize <= size) {
                newSize = newSize * 3 / 2;
            }
            Entry[] newUnusedEntries = new Entry[newSize];
            System.arraycopy(this.m_unusedEntries, 0, newUnusedEntries, 0, this.m_unusedEntries.length);
            this.m_unusedEntries = newUnusedEntries;
        }
        if ((entry = this.m_unusedEntries[size]) == null) {
            return new Entry(size);
        }
        this.m_unusedEntries[size] = entry.m_nextEntry;
        entry.m_nextEntry = null;
        return entry;
    }

    protected void leaveEntry(Entry<E> entry) {
        entry.m_nextEntry = this.m_unusedEntries[entry.size()];
        entry.m_previousEntry = null;
        this.m_unusedEntries[entry.size()] = entry;
    }

    protected static int getIndexFor(int hashCode, int tableLength) {
        return hashCode & tableLength - 1;
    }

    protected static class Entry<T>
    implements Serializable,
    Set<T> {
        private static final long serialVersionUID = -3850593656120645350L;
        protected final T[] m_table;
        protected int m_hashCode = 0;
        protected Entry<T> m_previousEntry;
        protected Entry<T> m_nextEntry;
        protected int m_referenceCount;
        protected boolean m_permanent;

        public Entry(int size) {
            this.m_table = (T[])new Object[size];
        }

        public void initialize(Collection<T> elements, int hashCode) {
            elements.toArray(this.m_table);
            this.m_hashCode = hashCode;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(T object) {
            throw new UnsupportedOperationException();
        }

        public boolean equalsTo(Set<T> elements) {
            if (this.m_table.length != elements.size()) {
                return false;
            }
            for (int index = this.m_table.length - 1; index >= 0; --index) {
                if (elements.contains(this.m_table[index])) continue;
                return false;
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            for (int index = this.m_table.length - 1; index >= 0; --index) {
                if (!this.m_table[index].equals(o)) continue;
                return true;
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object object : c) {
                if (this.contains(object)) continue;
                return false;
            }
            return true;
        }

        @Override
        public boolean isEmpty() {
            return this.m_table.length == 0;
        }

        @Override
        public Iterator<T> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return this.m_table.length;
        }

        @Override
        public Object[] toArray() {
            return this.m_table.clone();
        }

        @Override
        public <E> E[] toArray(E[] a) {
            System.arraycopy(this.m_table, 0, a, 0, this.m_table.length);
            return a;
        }

        @Override
        public int hashCode() {
            return this.m_hashCode;
        }

        @Override
        public boolean equals(Object that) {
            return this == that;
        }

        protected class EntryIterator
        implements Iterator<T> {
            protected int m_currentIndex = 0;

            @Override
            public boolean hasNext() {
                return this.m_currentIndex < Entry.this.m_table.length;
            }

            @Override
            public T next() {
                if (this.m_currentIndex >= Entry.this.m_table.length) {
                    throw new NoSuchElementException();
                }
                return Entry.this.m_table[this.m_currentIndex++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

    }

}

