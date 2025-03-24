package org.semanticweb.HermiT.datatypes.owlreal;

final class MinusInfinity
extends Number {
    private static final long serialVersionUID = -205551124673073593L;
    public static final MinusInfinity INSTANCE = new MinusInfinity();

    private MinusInfinity() {
    }

    public String toString() {
        return "-INF";
    }

    @Override
    public double doubleValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float floatValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int intValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long longValue() {
        throw new UnsupportedOperationException();
    }

    private Object readResolve() {
        return INSTANCE;
    }
}

