package org.semanticweb.HermiT.datatypes.doublenum;

import java.util.Collection;

public class DoubleInterval {
    protected final double m_lowerBoundInclusive;
    protected final double m_upperBoundInclusive;

    public DoubleInterval(double lowerBoundInclusive, double upperBoundInclusive) {
        assert (!DoubleInterval.isIntervalEmpty(lowerBoundInclusive, upperBoundInclusive));
        this.m_lowerBoundInclusive = lowerBoundInclusive;
        this.m_upperBoundInclusive = upperBoundInclusive;
    }

    public DoubleInterval intersectWith(DoubleInterval that) {
        double newUpperBoundInclusive;
        double newLowerBoundInclusive = DoubleInterval.isSmallerEqual(this.m_lowerBoundInclusive, that.m_lowerBoundInclusive) ? that.m_lowerBoundInclusive : this.m_lowerBoundInclusive;
        if (DoubleInterval.isIntervalEmpty(newLowerBoundInclusive, newUpperBoundInclusive = DoubleInterval.isSmallerEqual(this.m_upperBoundInclusive, that.m_upperBoundInclusive) ? this.m_upperBoundInclusive : that.m_upperBoundInclusive)) {
            return null;
        }
        if (this.isEqual(newLowerBoundInclusive, newUpperBoundInclusive)) {
            return this;
        }
        if (that.isEqual(newLowerBoundInclusive, newUpperBoundInclusive)) {
            return that;
        }
        return new DoubleInterval(newLowerBoundInclusive, newUpperBoundInclusive);
    }

    protected boolean isEqual(double lowerBoundInclusive, double upperBoundInclusive) {
        return DoubleInterval.areIdentical(this.m_lowerBoundInclusive, lowerBoundInclusive) && DoubleInterval.areIdentical(this.m_upperBoundInclusive, upperBoundInclusive);
    }

    public int subtractSizeFrom(int argument) {
        return DoubleInterval.subtractIntervalSizeFrom(this.m_lowerBoundInclusive, this.m_upperBoundInclusive, argument);
    }

    public boolean contains(double value) {
        return DoubleInterval.contains(this.m_lowerBoundInclusive, this.m_upperBoundInclusive, value);
    }

    public void enumerateNumbers(Collection<Object> numbers) {
        double number = this.m_lowerBoundInclusive;
        while (!DoubleInterval.areIdentical(number, this.m_upperBoundInclusive)) {
            numbers.add(number);
            number = DoubleInterval.nextDouble(number);
        }
        numbers.add(this.m_upperBoundInclusive);
    }

    public String toString() {
        String buffer = "DOUBLE[" +
                this.m_lowerBoundInclusive +
                ".." +
                this.m_upperBoundInclusive +
                ']';
        return buffer;
    }

    public static boolean isNaN(long bits) {
        return (bits & 9218868437227405312L) == 9218868437227405312L && (bits & 0xFFFFFFFFFFFFFL) != 0L;
    }

    protected static boolean isIntervalEmpty(double lowerBoundInclusive, double upperBoundInclusive) {
        return !DoubleInterval.isSmallerEqual(lowerBoundInclusive, upperBoundInclusive);
    }

    public static boolean areIdentical(double value1, double value2) {
        return Double.doubleToLongBits(value1) == Double.doubleToLongBits(value2);
    }

    public static double nextDouble(double value) {
        long newMagnitude;
        boolean positive;
        boolean newPositive;
        long bits = Double.doubleToRawLongBits(value);
        long magnitude = bits & Long.MAX_VALUE;
        boolean bl = positive = (bits & Long.MIN_VALUE) == 0L;
        if (DoubleInterval.isNaN(bits) || magnitude == 9218868437227405312L && positive) {
            return value;
        }
        if (positive) {
            newPositive = true;
            newMagnitude = magnitude + 1L;
        } else if (!positive && magnitude == 0L) {
            newPositive = true;
            newMagnitude = 0L;
        } else {
            newPositive = false;
            newMagnitude = magnitude - 1L;
        }
        long newBits = newMagnitude | (newPositive ? 0L : Long.MIN_VALUE);
        return Double.longBitsToDouble(newBits);
    }

    public static double previousDouble(double value) {
        long newMagnitude;
        boolean positive;
        boolean newPositive;
        long bits = Double.doubleToRawLongBits(value);
        long magnitude = bits & Long.MAX_VALUE;
        boolean bl = positive = (bits & Long.MIN_VALUE) == 0L;
        if (DoubleInterval.isNaN(bits) || magnitude == 9218868437227405312L && !positive) {
            return value;
        }
        if (!positive) {
            newPositive = false;
            newMagnitude = magnitude + 1L;
        } else if (positive && magnitude == 0L) {
            newPositive = false;
            newMagnitude = 0L;
        } else {
            newPositive = true;
            newMagnitude = magnitude - 1L;
        }
        long newBits = newMagnitude | (newPositive ? 0L : Long.MIN_VALUE);
        return Double.longBitsToDouble(newBits);
    }

    public static int subtractIntervalSizeFrom(double lowerBoundInclusive, double upperBoundInclusive, int argument) {
        boolean positiveUpperBoundInclusive;
        long magnitudeUpperBoundInclusive;
        long magnitudeLowerBoundInclusive;
        if (argument <= 0) {
            return 0;
        }
        long bitsLowerBoundInclusive = Double.doubleToRawLongBits(lowerBoundInclusive);
        long bitsUpperBoundInclusive = Double.doubleToRawLongBits(upperBoundInclusive);
        if (DoubleInterval.isNaN(bitsLowerBoundInclusive) || DoubleInterval.isNaN(bitsUpperBoundInclusive)) {
            return argument;
        }
        boolean positiveLowerBoundInclusive = (bitsLowerBoundInclusive & Long.MIN_VALUE) == 0L;
        if (!DoubleInterval.isSmallerEqual(positiveLowerBoundInclusive, magnitudeLowerBoundInclusive = bitsLowerBoundInclusive & Long.MAX_VALUE, positiveUpperBoundInclusive = (bitsUpperBoundInclusive & Long.MIN_VALUE) == 0L, magnitudeUpperBoundInclusive = bitsUpperBoundInclusive & Long.MAX_VALUE)) {
            return argument;
        }
        if (positiveLowerBoundInclusive && positiveUpperBoundInclusive) {
            long size = magnitudeUpperBoundInclusive - magnitudeLowerBoundInclusive + 1L;
            return (int)Math.max((long)argument - size, 0L);
        }
        if (!positiveLowerBoundInclusive && !positiveUpperBoundInclusive) {
            long size = magnitudeLowerBoundInclusive - magnitudeUpperBoundInclusive + 1L;
            return (int)Math.max((long)argument - size, 0L);
        }
        if (!positiveLowerBoundInclusive && positiveUpperBoundInclusive) {
            long startToMinusZero = magnitudeLowerBoundInclusive + 1L;
            if (startToMinusZero >= (long)argument) {
                return 0;
            }
            long plusZeroToEnd = 1L + magnitudeUpperBoundInclusive;
            if (plusZeroToEnd >= (long)(argument = (int)((long)argument - startToMinusZero))) {
                return 0;
            }
            return (int)((long)argument - plusZeroToEnd);
        }
        throw new IllegalStateException();
    }

    public static boolean contains(double startInclusive, double endInclusive, double value) {
        long bitsStart = Double.doubleToRawLongBits(startInclusive);
        long bitsEnd = Double.doubleToRawLongBits(endInclusive);
        long bitsValue = Double.doubleToRawLongBits(value);
        if (DoubleInterval.isNaN(bitsStart) || DoubleInterval.isNaN(bitsEnd) || DoubleInterval.isNaN(bitsValue)) {
            return false;
        }
        boolean positiveStart = (bitsStart & Long.MIN_VALUE) == 0L;
        boolean positiveEnd = (bitsEnd & Long.MIN_VALUE) == 0L;
        boolean positiveValue = (bitsValue & Long.MIN_VALUE) == 0L;
        long magnitudeStart = bitsStart & Long.MAX_VALUE;
        long magnitudeEnd = bitsEnd & Long.MAX_VALUE;
        long magnitudeValue = bitsValue & Long.MAX_VALUE;
        return DoubleInterval.isSmallerEqual(positiveStart, magnitudeStart, positiveValue, magnitudeValue) && DoubleInterval.isSmallerEqual(positiveValue, magnitudeValue, positiveEnd, magnitudeEnd);
    }

    public static boolean isSmallerEqual(double value1, double value2) {
        long bitsValue1 = Double.doubleToRawLongBits(value1);
        long bitsValue2 = Double.doubleToRawLongBits(value2);
        if (DoubleInterval.isNaN(bitsValue1) || DoubleInterval.isNaN(bitsValue2)) {
            return false;
        }
        boolean positiveValue1 = (bitsValue1 & Long.MIN_VALUE) == 0L;
        boolean positiveValue2 = (bitsValue2 & Long.MIN_VALUE) == 0L;
        long magnitudeValue1 = bitsValue1 & Long.MAX_VALUE;
        long magnitudeValue2 = bitsValue2 & Long.MAX_VALUE;
        return DoubleInterval.isSmallerEqual(positiveValue1, magnitudeValue1, positiveValue2, magnitudeValue2);
    }

    public static boolean isSmallerEqual(boolean positive1, long magnitude1, boolean positive2, long magnitude2) {
        if (positive1 && positive2) {
            return magnitude1 <= magnitude2;
        }
        if (!positive1 && positive2) {
            return true;
        }
        if (positive1 && !positive2) {
            return false;
        }
        return magnitude1 >= magnitude2;
    }
}

