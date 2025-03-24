package org.semanticweb.HermiT.datatypes.owlreal;

import java.util.Collection;

public class NumberInterval {
    protected final NumberRange m_baseRange;
    protected final NumberRange m_excludedRange;
    protected final Number m_lowerBound;
    protected final BoundType m_lowerBoundType;
    protected final Number m_upperBound;
    protected final BoundType m_upperBoundType;

    public NumberInterval(NumberRange baseRange, NumberRange excludedRange, Number lowerBound, BoundType lowerBoundType, Number upperBound, BoundType upperBoundType) {
        assert (!NumberInterval.isIntervalEmpty(baseRange, excludedRange, lowerBound, lowerBoundType, upperBound, upperBoundType));
        this.m_baseRange = baseRange;
        this.m_excludedRange = excludedRange;
        if (this.m_baseRange == NumberRange.INTEGER) {
            if (MinusInfinity.INSTANCE.equals(lowerBound)) {
                this.m_lowerBound = lowerBound;
                this.m_lowerBoundType = lowerBoundType;
            } else {
                this.m_lowerBound = Numbers.getNearestIntegerInBound(lowerBound, Numbers.BoundaryDirection.LOWER, lowerBoundType == BoundType.INCLUSIVE);
                this.m_lowerBoundType = BoundType.INCLUSIVE;
            }
            if (PlusInfinity.INSTANCE.equals(upperBound)) {
                this.m_upperBound = upperBound;
                this.m_upperBoundType = upperBoundType;
            } else {
                this.m_upperBound = Numbers.getNearestIntegerInBound(upperBound, Numbers.BoundaryDirection.UPPER, upperBoundType == BoundType.INCLUSIVE);
                this.m_upperBoundType = BoundType.INCLUSIVE;
            }
        } else {
            this.m_lowerBound = lowerBound;
            this.m_lowerBoundType = lowerBoundType;
            this.m_upperBound = upperBound;
            this.m_upperBoundType = upperBoundType;
        }
    }

    public NumberInterval intersectWith(NumberInterval that) {
        Number newUpperBound;
        NumberRange newExcludedRange;
        BoundType newUpperBoundType;
        BoundType newLowerBoundType;
        Number newLowerBound;
        NumberRange newBaseRange = NumberRange.intersection(this.m_baseRange, that.m_baseRange);
        if (NumberRange.isSubsetOf(newBaseRange, newExcludedRange = NumberRange.union(this.m_excludedRange, that.m_excludedRange))) {
            return null;
        }
        int lowerBoundComparison = Numbers.compare(this.m_lowerBound, that.m_lowerBound);
        if (lowerBoundComparison < 0) {
            newLowerBound = that.m_lowerBound;
            newLowerBoundType = that.m_lowerBoundType;
        } else if (lowerBoundComparison > 0) {
            newLowerBound = this.m_lowerBound;
            newLowerBoundType = this.m_lowerBoundType;
        } else {
            newLowerBound = this.m_lowerBound;
            newLowerBoundType = BoundType.getMoreRestrictive(this.m_lowerBoundType, that.m_lowerBoundType);
        }
        int upperBoundComparison = Numbers.compare(this.m_upperBound, that.m_upperBound);
        if (upperBoundComparison < 0) {
            newUpperBound = this.m_upperBound;
            newUpperBoundType = this.m_upperBoundType;
        } else if (upperBoundComparison > 0) {
            newUpperBound = that.m_upperBound;
            newUpperBoundType = that.m_upperBoundType;
        } else {
            newUpperBound = this.m_upperBound;
            newUpperBoundType = BoundType.getMoreRestrictive(this.m_upperBoundType, that.m_upperBoundType);
        }
        if (NumberInterval.isIntervalEmpty(newBaseRange, newExcludedRange, newLowerBound, newLowerBoundType, newUpperBound, newUpperBoundType)) {
            return null;
        }
        if (this.isEqual(newBaseRange, newExcludedRange, newLowerBound, newLowerBoundType, newUpperBound, newUpperBoundType)) {
            return this;
        }
        if (that.isEqual(newBaseRange, newExcludedRange, newLowerBound, newLowerBoundType, newUpperBound, newUpperBoundType)) {
            return that;
        }
        return new NumberInterval(newBaseRange, newExcludedRange, newLowerBound, newLowerBoundType, newUpperBound, newUpperBoundType);
    }

    protected boolean isEqual(NumberRange baseRange, NumberRange excludedRange, Number lowerBound, BoundType lowerBoundType, Number upperBound, BoundType upperBoundType) {
        return this.m_baseRange.equals(baseRange) && this.m_excludedRange.equals(excludedRange) && this.m_lowerBound.equals(lowerBound) && this.m_lowerBoundType.equals(lowerBoundType) && this.m_upperBound.equals(upperBound) && this.m_upperBoundType.equals(upperBoundType);
    }

    public int subtractSizeFrom(int argument) {
        if (argument <= 0) {
            return 0;
        }
        if (this.m_lowerBound.equals(this.m_upperBound)) {
            return argument - 1;
        }
        if (this.m_baseRange.isDense()) {
            return 0;
        }
        if (MinusInfinity.INSTANCE.equals(this.m_lowerBound) || PlusInfinity.INSTANCE.equals(this.m_upperBound)) {
            return 0;
        }
        return Numbers.subtractIntegerIntervalSizeFrom(this.m_lowerBound, this.m_upperBound, argument);
    }

    public boolean containsNumber(Number number) {
        NumberRange mostSpecificRange = NumberRange.getMostSpecificRange(number);
        if (!NumberRange.isSubsetOf(mostSpecificRange, this.m_baseRange) || NumberRange.isSubsetOf(mostSpecificRange, this.m_excludedRange)) {
            return false;
        }
        int lowerBoundComparison = Numbers.compare(this.m_lowerBound, number);
        if (lowerBoundComparison > 0 || lowerBoundComparison == 0 && this.m_lowerBoundType == BoundType.EXCLUSIVE) {
            return false;
        }
        int upperBoundComparison = Numbers.compare(this.m_upperBound, number);
        return upperBoundComparison >= 0 && (upperBoundComparison != 0 || this.m_upperBoundType != BoundType.EXCLUSIVE);
    }

    public void enumerateNumbers(Collection<Object> numbers) {
        if (this.m_lowerBound.equals(this.m_upperBound)) {
            numbers.add(this.m_lowerBound);
        } else {
            if (this.m_baseRange.isDense()) {
                throw new IllegalStateException("The data range is infinite.");
            }
            if (MinusInfinity.INSTANCE.equals(this.m_lowerBound) || PlusInfinity.INSTANCE.equals(this.m_upperBound)) {
                throw new IllegalStateException("The data range is infinite.");
            }
            Number integer = this.m_lowerBound;
            while (!integer.equals(this.m_upperBound)) {
                numbers.add(integer);
                integer = Numbers.nextInteger(integer);
            }
            numbers.add(this.m_upperBound);
        }
    }

    protected static boolean isIntervalEmpty(NumberRange baseRange, NumberRange excludedRange, Number lowerBound, BoundType lowerBoundType, Number upperBound, BoundType upperBoundType) {
        if (NumberRange.isSubsetOf(baseRange, excludedRange)) {
            return true;
        }
        int boundComparison = Numbers.compare(lowerBound, upperBound);
        if (boundComparison > 0) {
            return true;
        }
        if (boundComparison == 0) {
            if (lowerBoundType == BoundType.EXCLUSIVE || upperBoundType == BoundType.EXCLUSIVE || MinusInfinity.INSTANCE.equals(lowerBound) || PlusInfinity.INSTANCE.equals(lowerBound)) {
                return true;
            }
            NumberRange mostSpecificRange = NumberRange.getMostSpecificRange(lowerBound);
            return !NumberRange.isSubsetOf(mostSpecificRange, baseRange) || NumberRange.isSubsetOf(mostSpecificRange, excludedRange);
        }
        if (baseRange.isDense()) {
            return false;
        }
        if (MinusInfinity.INSTANCE.equals(lowerBound) || PlusInfinity.INSTANCE.equals(upperBound)) {
            return false;
        }
        Number lowerBoundInclusive = Numbers.getNearestIntegerInBound(lowerBound, Numbers.BoundaryDirection.LOWER, lowerBoundType == BoundType.INCLUSIVE);
        Number upperBoundInclusive = Numbers.getNearestIntegerInBound(upperBound, Numbers.BoundaryDirection.UPPER, upperBoundType == BoundType.INCLUSIVE);
        return Numbers.compare(lowerBoundInclusive, upperBoundInclusive) > 0;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.m_baseRange.toString());
        if (this.m_excludedRange != NumberRange.NOTHING) {
            buffer.append('\\');
            buffer.append(this.m_excludedRange.toString());
        }
        if (this.m_lowerBoundType == BoundType.INCLUSIVE) {
            buffer.append('[');
        } else {
            buffer.append('<');
        }
        buffer.append(this.m_lowerBound.toString());
        buffer.append(" .. ");
        buffer.append(this.m_upperBound.toString());
        if (this.m_upperBoundType == BoundType.INCLUSIVE) {
            buffer.append(']');
        } else {
            buffer.append('>');
        }
        return buffer.toString();
    }
}

