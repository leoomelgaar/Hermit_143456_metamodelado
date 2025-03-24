package org.semanticweb.HermiT.datatypes.owlreal;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Numbers {
    public static boolean isValidNumber(Number n) {
        return n instanceof Integer || n instanceof Long || n instanceof BigInteger || n instanceof BigDecimal || n instanceof BigRational;
    }

    public static Number parseInteger(String string) throws NumberFormatException {
        try {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException numberFormatException) {
            try {
                return Long.parseLong(string);
            }
            catch (NumberFormatException numberFormatException2) {
                return new BigInteger(string);
            }
        }
    }

    public static Number parseDecimal(String string) throws NumberFormatException {
        BigDecimal decimal = new BigDecimal(string);
        try {
            return decimal.intValueExact();
        }
        catch (ArithmeticException arithmeticException) {
            try {
                return decimal.longValueExact();
            }
            catch (ArithmeticException arithmeticException2) {
                try {
                    return decimal.toBigIntegerExact();
                }
                catch (ArithmeticException arithmeticException3) {
                    return decimal.stripTrailingZeros();
                }
            }
        }
    }

    public static Number parseRational(String string) throws NumberFormatException {
        int divideIndex = string.indexOf(47);
        if (divideIndex == -1) {
            throw new NumberFormatException("The string does not contain /.");
        }
        int startIndex = 0;
        if (string.startsWith("+")) {
            startIndex = 1;
        }
        BigInteger numerator = new BigInteger(string.substring(startIndex, divideIndex));
        BigInteger denominator = new BigInteger(string.substring(divideIndex + 1));
        if (denominator.compareTo(BigInteger.ZERO) <= 0) {
            throw new NumberFormatException("Invalid denumerator of the rational.");
        }
        BigInteger gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        if ((denominator = denominator.divide(gcd)).equals(BigInteger.ONE)) {
            int numeratorBitCount = numerator.bitCount();
            if (numeratorBitCount <= 32) {
                return numerator.intValue();
            }
            if (numeratorBitCount <= 64) {
                return numerator.longValue();
            }
            return numerator;
        }
        try {
            return new BigDecimal(numerator).divide(new BigDecimal(denominator));
        }
        catch (ArithmeticException numeratorBitCount) {
            return new BigRational(numerator, denominator);
        }
    }

    public static int compare(Number n1, Number n2) {
        if (n1.equals(n2)) {
            return 0;
        }
        if (n1.equals(MinusInfinity.INSTANCE) || n2.equals(PlusInfinity.INSTANCE)) {
            return -1;
        }
        if (n1.equals(PlusInfinity.INSTANCE) || n2.equals(MinusInfinity.INSTANCE)) {
            return 1;
        }
        NumberType typeN1 = NumberType.getNumberTypeFor(n1);
        NumberType typeN2 = NumberType.getNumberTypeFor(n2);
        NumberType maxType = NumberType.getMaxNumberType(typeN1, typeN2);
        switch (maxType) {
            case INTEGER: {
                int iv1 = n1.intValue();
                int iv2 = n2.intValue();
                return iv1 < iv2 ? -1 : (iv1 == iv2 ? 0 : 1);
            }
            case LONG: {
                long lv1 = n1.longValue();
                long lv2 = n2.longValue();
                return lv1 < lv2 ? -1 : (lv1 == lv2 ? 0 : 1);
            }
            case BIG_INTEGER: {
                BigInteger bi1 = Numbers.toBigInteger(n1, typeN1);
                BigInteger bi2 = Numbers.toBigInteger(n2, typeN2);
                return bi1.compareTo(bi2);
            }
            case BIG_DECIMAL: {
                BigDecimal bd1 = Numbers.toBigDecimal(n1, typeN1);
                BigDecimal bd2 = Numbers.toBigDecimal(n2, typeN2);
                return bd1.compareTo(bd2);
            }
            case BIG_RATIONAL: {
                BigRational br1 = Numbers.toBigRational(n1, typeN1);
                BigRational br2 = Numbers.toBigRational(n2, typeN2);
                return br1.compareTo(br2);
            }
        }
        throw new IllegalArgumentException();
    }

    protected static BigInteger toBigInteger(Number n, NumberType nType) {
        switch (nType) {
            case INTEGER: 
            case LONG: {
                return BigInteger.valueOf(n.longValue());
            }
            case BIG_INTEGER: {
                return (BigInteger)n;
            }
        }
        throw new IllegalArgumentException();
    }

    protected static BigDecimal toBigDecimal(Number n, NumberType nType) {
        switch (nType) {
            case INTEGER: 
            case LONG: {
                return BigDecimal.valueOf(n.longValue());
            }
            case BIG_INTEGER: {
                return new BigDecimal((BigInteger)n);
            }
            case BIG_DECIMAL: {
                return (BigDecimal)n;
            }
        }
        throw new IllegalArgumentException();
    }

    protected static BigRational toBigRational(Number n, NumberType nType) {
        switch (nType) {
            case INTEGER: 
            case LONG: {
                return new BigRational(BigInteger.valueOf(n.longValue()), BigInteger.ONE);
            }
            case BIG_INTEGER: {
                return new BigRational((BigInteger)n, BigInteger.ONE);
            }
            case BIG_DECIMAL: {
                BigDecimal decimal = (BigDecimal)n;
                assert (decimal.scale() > 0);
                return new BigRational(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()));
            }
            case BIG_RATIONAL: {
                return (BigRational)n;
            }
        }
        throw new IllegalArgumentException();
    }

    public static Number getNearestIntegerInBound(Number bound, BoundaryDirection boundaryDirection, boolean boundIsInclusive) {
        switch (NumberType.getNumberTypeFor(bound)) {
            case INTEGER: {
                if (boundIsInclusive) {
                    return bound;
                }
                if (BoundaryDirection.LOWER.equals(boundaryDirection)) {
                    int value = bound.intValue();
                    if (value == Integer.MAX_VALUE) {
                        return (long)value + 1L;
                    }
                    return value + 1;
                }
                int value = bound.intValue();
                if (value == Integer.MIN_VALUE) {
                    return (long)value - 11L;
                }
                return value - 1;
            }
            case LONG: {
                if (boundIsInclusive) {
                    return bound;
                }
                if (BoundaryDirection.LOWER.equals(boundaryDirection)) {
                    long value = bound.longValue();
                    if (value == Long.MAX_VALUE) {
                        return BigInteger.valueOf(value).add(BigInteger.ONE);
                    }
                    return value + 1L;
                }
                long value = bound.longValue();
                if (value == Long.MIN_VALUE) {
                    return BigInteger.valueOf(value).subtract(BigInteger.ONE);
                }
                return value - 1L;
            }
            case BIG_INTEGER: {
                if (boundIsInclusive) {
                    return bound;
                }
                if (BoundaryDirection.LOWER.equals(boundaryDirection)) {
                    return ((BigInteger)bound).add(BigInteger.ONE);
                }
                return ((BigInteger)bound).subtract(BigInteger.ONE);
            }
            case BIG_DECIMAL: {
                int biBitCount;
                BigDecimal bd = (BigDecimal)bound;
                assert (bd.scale() > 0);
                BigInteger bi = bd.toBigInteger();
                if (BoundaryDirection.LOWER.equals(boundaryDirection)) {
                    if (bd.compareTo(BigDecimal.ZERO) > 0) {
                        bi = bi.add(BigInteger.ONE);
                    }
                } else if (bd.compareTo(BigDecimal.ZERO) < 0) {
                    bi = bi.subtract(BigInteger.ONE);
                }
                if ((biBitCount = bi.bitCount()) <= 32) {
                    return bi.intValue();
                }
                if (biBitCount <= 64) {
                    return bi.longValue();
                }
                return bi;
            }
            case BIG_RATIONAL: {
                int quotientBitCount;
                BigRational br = (BigRational)bound;
                BigDecimal numerator = new BigDecimal(br.getNumerator());
                BigDecimal denominator = new BigDecimal(br.getDenominator());
                BigInteger quotient = numerator.divideToIntegralValue(denominator).toBigInteger();
                if (BoundaryDirection.LOWER.equals(boundaryDirection)) {
                    if (numerator.compareTo(BigDecimal.ZERO) > 0) {
                        quotient = quotient.add(BigInteger.ONE);
                    }
                } else if (numerator.compareTo(BigDecimal.ZERO) < 0) {
                    quotient = quotient.subtract(BigInteger.ONE);
                }
                if ((quotientBitCount = quotient.bitCount()) <= 32) {
                    return quotient.intValue();
                }
                if (quotientBitCount <= 64) {
                    return quotient.longValue();
                }
                return quotient;
            }
        }
        throw new IllegalArgumentException();
    }

    public static int subtractIntegerIntervalSizeFrom(Number lowerBoundInclusive, Number upperBoundInclusive, int argument) {
        if (argument <= 0) {
            return 0;
        }
        if (lowerBoundInclusive.equals(upperBoundInclusive)) {
            return argument;
        }
        NumberType typeLowerBound = NumberType.getNumberTypeFor(lowerBoundInclusive);
        NumberType typeUpperBound = NumberType.getNumberTypeFor(upperBoundInclusive);
        NumberType maxType = NumberType.getMaxNumberType(typeLowerBound, typeUpperBound);
        switch (maxType) {
            case INTEGER: {
                int size = upperBoundInclusive.intValue() - lowerBoundInclusive.intValue() + 1;
                if (size <= 0) {
                    return 0;
                }
                return Math.max(argument - size, 0);
            }
            case LONG: {
                long size = upperBoundInclusive.longValue() - lowerBoundInclusive.longValue() + 1L;
                if (size <= 0L) {
                    return 0;
                }
                return (int)Math.max((long)argument - size, 0L);
            }
            case BIG_INTEGER: {
                BigInteger leftover = BigInteger.valueOf(argument).subtract(Numbers.toBigInteger(upperBoundInclusive, typeUpperBound)).add(Numbers.toBigInteger(lowerBoundInclusive, typeLowerBound)).subtract(BigInteger.ONE);
                if (leftover.compareTo(BigInteger.ZERO) <= 0) {
                    return 0;
                }
                return leftover.intValue();
            }
        }
        throw new IllegalArgumentException();
    }

    public static Number nextInteger(Number integer) {
        switch (NumberType.getNumberTypeFor(integer)) {
            case INTEGER: {
                int value = integer.intValue();
                if (value == Integer.MAX_VALUE) {
                    return (long)value + 1L;
                }
                return value + 1;
            }
            case LONG: {
                long value = integer.longValue();
                if (value == Long.MAX_VALUE) {
                    return BigInteger.valueOf(value).add(BigInteger.ONE);
                }
                return value + 1L;
            }
            case BIG_INTEGER: {
                return ((BigInteger)integer).add(BigInteger.ONE);
            }
        }
        throw new IllegalArgumentException();
    }

    public enum BoundaryDirection {
        UPPER,
        LOWER

    }

    protected enum NumberType {
        INTEGER,
        LONG,
        BIG_INTEGER,
        BIG_DECIMAL,
        BIG_RATIONAL;
        

        private static NumberType getMaxNumberType(NumberType typeN1, NumberType typeN2) {
            return typeN1.ordinal() >= typeN2.ordinal() ? typeN1 : typeN2;
        }

        private static NumberType getNumberTypeFor(Number n) {
            if (n instanceof Integer) {
                return INTEGER;
            }
            if (n instanceof Long) {
                return LONG;
            }
            if (n instanceof BigInteger) {
                return BIG_INTEGER;
            }
            if (n instanceof BigDecimal) {
                return BIG_DECIMAL;
            }
            if (n instanceof BigRational) {
                return BIG_RATIONAL;
            }
            throw new IllegalArgumentException();
        }
    }

}

