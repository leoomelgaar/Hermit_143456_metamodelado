package org.semanticweb.HermiT.tableau;

import java.io.Serializable;

public final class TupleTable
implements Serializable {
    private static final long serialVersionUID = -7712458276004062803L;
    private static final int PAGE_SIZE = 512;
    private final int m_arity;
    Page[] m_pages;
    private int m_numberOfPages;
    private int m_tupleCapacity;
    private int m_firstFreeTupleIndex;

    public TupleTable(int arity) {
        this.m_arity = arity;
        this.clear();
    }

    public int sizeInMemory() {
        int size = this.m_pages.length * 4;
        for (int i = this.m_pages.length - 1; i >= 0; --i) {
            if (this.m_pages[i] == null) continue;
            size += this.m_pages[i].sizeInMemory();
        }
        return size;
    }

    public int getFirstFreeTupleIndex() {
        return this.m_firstFreeTupleIndex;
    }

    public int addTuple(Object[] tupleBuffer) {
        int newTupleIndex = this.m_firstFreeTupleIndex;
        if (newTupleIndex == this.m_tupleCapacity) {
            if (this.m_numberOfPages == this.m_pages.length) {
                Page[] newPages = new Page[this.m_numberOfPages * 3 / 2];
                System.arraycopy(this.m_pages, 0, newPages, 0, this.m_numberOfPages);
                this.m_pages = newPages;
            }
            this.m_pages[this.m_numberOfPages++] = new Page(this.m_arity);
            this.m_tupleCapacity += PAGE_SIZE;
        }
        this.m_pages[newTupleIndex / PAGE_SIZE].storeTuple(newTupleIndex % PAGE_SIZE * this.m_arity, tupleBuffer);
        ++this.m_firstFreeTupleIndex;
        
        // Debug logging for large indices
        if (this.m_firstFreeTupleIndex > 1000) {
            System.out.println("TupleTable.addTuple: firstFreeTupleIndex is now " + this.m_firstFreeTupleIndex + 
                             " (added at index " + newTupleIndex + ")");
        }
        
        return newTupleIndex;
    }

    public boolean tupleEquals(Object[] tupleBuffer, int tupleIndex, int compareLength) {
        return this.m_pages[tupleIndex / PAGE_SIZE].tupleEquals(tupleBuffer, tupleIndex % PAGE_SIZE * this.m_arity, compareLength);
    }

    public boolean tupleEquals(Object[] tupleBuffer, int[] positionIndexes, int tupleIndex, int compareLength) {
        return this.m_pages[tupleIndex / PAGE_SIZE].tupleEquals(tupleBuffer, positionIndexes, tupleIndex % PAGE_SIZE * this.m_arity, compareLength);
    }

    public void retrieveTuple(Object[] tupleBuffer, int tupleIndex) {
        this.m_pages[tupleIndex / PAGE_SIZE].retrieveTuple(tupleIndex % PAGE_SIZE * this.m_arity, tupleBuffer);
    }

    public Object getTupleObject(int tupleIndex, int objectIndex) {
        assert (objectIndex < this.m_arity);
        return this.m_pages[tupleIndex / PAGE_SIZE].m_objects[tupleIndex % PAGE_SIZE * this.m_arity + objectIndex];
    }

    public void setTupleObject(int tupleIndex, int objectIndex, Object object) {
        this.m_pages[tupleIndex / PAGE_SIZE].m_objects[tupleIndex % PAGE_SIZE * this.m_arity + objectIndex] = object;
    }

    public void truncate(int newFirstFreeTupleIndex) {
        System.out.println("TupleTable.truncate: changing firstFreeTupleIndex from " + 
                          this.m_firstFreeTupleIndex + " to " + newFirstFreeTupleIndex);
        this.m_firstFreeTupleIndex = newFirstFreeTupleIndex;
    }

    public void nullifyTuple(int tupleIndex) {
        this.m_pages[tupleIndex / PAGE_SIZE].nullifyTuple(tupleIndex % PAGE_SIZE * this.m_arity);
    }

    public void clear() {
        System.out.println("TupleTable.clear: resetting firstFreeTupleIndex from " + 
                          this.m_firstFreeTupleIndex + " to 0");
        this.m_pages = new Page[10];
        this.m_numberOfPages = 1;
        this.m_pages[0] = new Page(this.m_arity);
        this.m_tupleCapacity = this.m_numberOfPages * PAGE_SIZE;
        this.m_firstFreeTupleIndex = 0;
    }

    protected static final class Page
    implements Serializable {
        private static final long serialVersionUID = 2239482172592108644L;
        public final int m_arity;
        public final Object[] m_objects;

        public Page(int arity) {
            this.m_arity = arity;
            this.m_objects = new Object[this.m_arity * PAGE_SIZE];
        }

        public int sizeInMemory() {
            return this.m_objects.length * 4;
        }

        public void storeTuple(int tupleStartIndex, Object[] tupleBuffer) {
            System.arraycopy(tupleBuffer, 0, this.m_objects, tupleStartIndex, tupleBuffer.length);
        }

        public void retrieveTuple(int tupleStartIndex, Object[] tupleBuffer) {
            System.arraycopy(this.m_objects, tupleStartIndex, tupleBuffer, 0, tupleBuffer.length);
        }

        public void nullifyTuple(int tupleStartIndex) {
            for (int index = 0; index < this.m_arity; ++index) {
                this.m_objects[tupleStartIndex + index] = null;
            }
        }

        public boolean tupleEquals(Object[] tupleBuffer, int tupleStartIndex, int compareLength) {
            int sourceIndex = compareLength - 1;
            int targetIndex = tupleStartIndex + sourceIndex;
            while (sourceIndex >= 0) {
                if (!tupleBuffer[sourceIndex].equals(this.m_objects[targetIndex])) {
                    return false;
                }
                --sourceIndex;
                --targetIndex;
            }
            return true;
        }

        public boolean tupleEquals(Object[] tupleBuffer, int[] positionIndexes, int tupleStartIndex, int compareLength) {
            int sourceIndex = compareLength - 1;
            int targetIndex = tupleStartIndex + sourceIndex;
            while (sourceIndex >= 0) {
                if (!tupleBuffer[positionIndexes[sourceIndex]].equals(this.m_objects[targetIndex])) {
                    return false;
                }
                --sourceIndex;
                --targetIndex;
            }
            return true;
        }
    }

}

