package org.semanticweb.HermiT.datatypes.floatnum;

import java.util.Collection;

public class FloatInterval {
    protected final float m_lowerBoundInclusive;
    protected final float m_upperBoundInclusive;

    public FloatInterval(float lowerBoundInclusive, float upperBoundInclusive) {
        assert (!FloatInterval.isIntervalEmpty(lowerBoundInclusive, upperBoundInclusive));
        this.m_lowerBoundInclusive = lowerBoundInclusive;
        this.m_upperBoundInclusive = upperBoundInclusive;
    }

    public FloatInterval intersectWith(FloatInterval that) {
        float newUpperBoundInclusive;
        float newLowerBoundInclusive = FloatInterval.isSmallerEqual(this.m_lowerBoundInclusive, that.m_lowerBoundInclusive) ? that.m_lowerBoundInclusive : this.m_lowerBoundInclusive;
        if (FloatInterval.isIntervalEmpty(newLowerBoundInclusive, newUpperBoundInclusive = FloatInterval.isSmallerEqual(this.m_upperBoundInclusive, that.m_upperBoundInclusive) ? this.m_upperBoundInclusive : that.m_upperBoundInclusive)) {
            return null;
        }
        if (this.isEqual(newLowerBoundInclusive, newUpperBoundInclusive)) {
            return this;
        }
        if (that.isEqual(newLowerBoundInclusive, newUpperBoundInclusive)) {
            return that;
        }
        return new FloatInterval(newLowerBoundInclusive, newUpperBoundInclusive);
    }

    protected boolean isEqual(float lowerBoundInclusive, float upperBoundInclusive) {
        return FloatInterval.areIdentical(this.m_lowerBoundInclusive, lowerBoundInclusive) && FloatInterval.areIdentical(this.m_upperBoundInclusive, upperBoundInclusive);
    }

    public int subtractSizeFrom(int argument) {
        return FloatInterval.subtractIntervalSizeFrom(this.m_lowerBoundInclusive, this.m_upperBoundInclusive, argument);
    }

    public boolean contains(float value) {
        return FloatInterval.contains(this.m_lowerBoundInclusive, this.m_upperBoundInclusive, value);
    }

    public void enumerateNumbers(Collection<Object> numbers) {
        float number = this.m_lowerBoundInclusive;
        while (!FloatInterval.areIdentical(number, this.m_upperBoundInclusive)) {
            numbers.add(Float.valueOf(number));
            number = FloatInterval.nextFloat(number);
        }
        numbers.add(Float.valueOf(this.m_upperBoundInclusive));
    }

    public String toString() {
        String buffer = "FLOAT[" +
                this.m_lowerBoundInclusive +
                ".." +
                this.m_upperBoundInclusive +
                ']';
        return buffer;
    }

    public static boolean isNaN(int bits) {
        return (bits & 2139095040) == 2139095040 && (bits & 4194303) != 0;
    }

    protected static boolean isIntervalEmpty(float lowerBoundInclusive, float upperBoundInclusive) {
        return !FloatInterval.isSmallerEqual(lowerBoundInclusive, upperBoundInclusive);
    }

    public static boolean areIdentical(float value1, float value2) {
        return Float.floatToIntBits(value1) == Float.floatToIntBits(value2);
    }

    public static float nextFloat(float value) {
        boolean positive;
        boolean newPositive;
        int newMagnitude;
        int bits = Float.floatToIntBits(value);
        int magnitude = bits & Integer.MAX_VALUE;
        boolean bl = positive = (bits & Integer.MIN_VALUE) == 0;
        if (FloatInterval.isNaN(bits) || magnitude == 2139095040 && positive) {
            return value;
        }
        if (positive) {
            newPositive = true;
            newMagnitude = magnitude + 1;
        } else if (!positive && magnitude == 0) {
            newPositive = true;
            newMagnitude = 0;
        } else {
            newPositive = false;
            newMagnitude = magnitude - 1;
        }
        int newBits = newMagnitude | (newPositive ? 0 : Integer.MIN_VALUE);
        return Float.intBitsToFloat(newBits);
    }

    public static float previousFloat(float value) {
        boolean positive;
        boolean newPositive;
        int newMagnitude;
        int bits = Float.floatToIntBits(value);
        int magnitude = bits & Integer.MAX_VALUE;
        boolean bl = positive = (bits & Integer.MIN_VALUE) == 0;
        if (FloatInterval.isNaN(bits) || magnitude == 2139095040 && !positive) {
            return value;
        }
        if (!positive) {
            newPositive = false;
            newMagnitude = magnitude + 1;
        } else if (positive && magnitude == 0) {
            newPositive = false;
            newMagnitude = 0;
        } else {
            newPositive = true;
            newMagnitude = magnitude - 1;
        }
        int newBits = newMagnitude | (newPositive ? 0 : Integer.MIN_VALUE);
        return Float.intBitsToFloat(newBits);
    }

    public static int subtractIntervalSizeFrom(float lowerBoundInclusive, float upperBoundInclusive, int argument) {
        int magnitudeUpperBoundInclusive;
        int magnitudeLowerBoundInclusive;
        boolean positiveUpperBoundInclusive;
        if (argument <= 0) {
            return 0;
        }
        int bitsLowerBoundInclusive = Float.floatToIntBits(lowerBoundInclusive);
        int bitsUpperBoundInclusive = Float.floatToIntBits(upperBoundInclusive);
        if (FloatInterval.isNaN(bitsLowerBoundInclusive) || FloatInterval.isNaN(bitsUpperBoundInclusive)) {
            return argument;
        }
        boolean positiveLowerBoundInclusive = (bitsLowerBoundInclusive & Integer.MIN_VALUE) == 0;
        if (!FloatInterval.isSmallerEqual(positiveLowerBoundInclusive, magnitudeLowerBoundInclusive = bitsLowerBoundInclusive & Integer.MAX_VALUE, positiveUpperBoundInclusive = (bitsUpperBoundInclusive & Integer.MIN_VALUE) == 0, magnitudeUpperBoundInclusive = bitsUpperBoundInclusive & Integer.MAX_VALUE)) {
            return argument;
        }
        if (positiveLowerBoundInclusive && positiveUpperBoundInclusive) {
            int size = magnitudeUpperBoundInclusive - magnitudeLowerBoundInclusive + 1;
            return Math.max(argument - size, 0);
        }
        if (!positiveLowerBoundInclusive && !positiveUpperBoundInclusive) {
            int size = magnitudeLowerBoundInclusive - magnitudeUpperBoundInclusive + 1;
            return Math.max(argument - size, 0);
        }
        if (!positiveLowerBoundInclusive && positiveUpperBoundInclusive) {
            int startToMinusZero = magnitudeLowerBoundInclusive + 1;
            if (startToMinusZero >= argument) {
                return 0;
            }
            int plusZeroToEnd = 1 + magnitudeUpperBoundInclusive;
            if (plusZeroToEnd >= (argument -= startToMinusZero)) {
                return 0;
            }
            return argument - plusZeroToEnd;
        }
        throw new IllegalStateException();
    }

    public static boolean contains(float startInclusive, float endInclusive, float value) {
        int bitsStart = Float.floatToIntBits(startInclusive);
        int bitsEnd = Float.floatToIntBits(endInclusive);
        int bitsValue = Float.floatToIntBits(value);
        if (FloatInterval.isNaN(bitsStart) || FloatInterval.isNaN(bitsEnd) || FloatInterval.isNaN(bitsValue)) {
            return false;
        }
        boolean positiveStart = (bitsStart & Integer.MIN_VALUE) == 0;
        boolean positiveEnd = (bitsEnd & Integer.MIN_VALUE) == 0;
        boolean positiveValue = (bitsValue & Integer.MIN_VALUE) == 0;
        int magnitudeStart = bitsStart & Integer.MAX_VALUE;
        int magnitudeEnd = bitsEnd & Integer.MAX_VALUE;
        int magnitudeValue = bitsValue & Integer.MAX_VALUE;
        return FloatInterval.isSmallerEqual(positiveStart, magnitudeStart, positiveValue, magnitudeValue) && FloatInterval.isSmallerEqual(positiveValue, magnitudeValue, positiveEnd, magnitudeEnd);
    }

    public static boolean isSmallerEqual(float value1, float value2) {
        int bitsValue1 = Float.floatToIntBits(value1);
        int bitsValue2 = Float.floatToIntBits(value2);
        if (FloatInterval.isNaN(bitsValue1) || FloatInterval.isNaN(bitsValue2)) {
            return false;
        }
        boolean positiveValue1 = (bitsValue1 & Integer.MIN_VALUE) == 0;
        boolean positiveValue2 = (bitsValue2 & Integer.MIN_VALUE) == 0;
        int magnitudeValue1 = bitsValue1 & Integer.MAX_VALUE;
        int magnitudeValue2 = bitsValue2 & Integer.MAX_VALUE;
        return FloatInterval.isSmallerEqual(positiveValue1, magnitudeValue1, positiveValue2, magnitudeValue2);
    }

    public static boolean isSmallerEqual(boolean positive1, int magnitude1, boolean positive2, int magnitude2) {
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

