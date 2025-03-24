package org.semanticweb.HermiT.datatypes.rdfplainliteral;

import java.util.Collection;

public class RDFPlainLiteralLengthInterval {
    public static final int CHARACTER_COUNT = 1112033;
    protected final LanguageTagMode m_languageTagMode;
    protected final int m_minLength;
    protected final int m_maxLength;

    public RDFPlainLiteralLengthInterval(LanguageTagMode languageTagMode, int minLength, int maxLength) {
        assert (!RDFPlainLiteralLengthInterval.isIntervalEmpty(minLength, maxLength));
        this.m_languageTagMode = languageTagMode;
        this.m_minLength = minLength;
        this.m_maxLength = maxLength;
    }

    public RDFPlainLiteralLengthInterval intersectWith(RDFPlainLiteralLengthInterval that) {
        int newMaxLength;
        if (this.m_languageTagMode != that.m_languageTagMode) {
            return null;
        }
        int newMinLength = Math.max(this.m_minLength, that.m_minLength);
        if (RDFPlainLiteralLengthInterval.isIntervalEmpty(newMinLength, newMaxLength = Math.min(this.m_maxLength, that.m_maxLength))) {
            return null;
        }
        if (this.isEqual(this.m_languageTagMode, newMinLength, newMaxLength)) {
            return this;
        }
        if (that.isEqual(this.m_languageTagMode, newMinLength, newMaxLength)) {
            return that;
        }
        return new RDFPlainLiteralLengthInterval(this.m_languageTagMode, newMinLength, newMaxLength);
    }

    protected boolean isEqual(LanguageTagMode languageTagMode, int minLength, int maxLength) {
        return this.m_languageTagMode == languageTagMode && this.m_minLength == minLength && this.m_maxLength == maxLength;
    }

    public int subtractSizeFrom(int argument) {
        if (argument <= 0 || this.m_maxLength == Integer.MAX_VALUE || this.m_languageTagMode == LanguageTagMode.PRESENT) {
            return 0;
        }
        if (this.m_minLength >= 4 || this.m_maxLength >= 4) {
            return 0;
        }
        long size = this.getNumberOfValuesOfLength(this.m_maxLength) - this.getNumberOfValuesOfLength(this.m_minLength - 1);
        return (int)Math.max((long)argument - size, 0L);
    }

    protected long getNumberOfValuesOfLength(int length) {
        if (length < 0) {
            return 0L;
        }
        long valuesOfLength = 1L;
        long total = 1L;
        for (int i = 1; i <= length; ++i) {
            total += (valuesOfLength *= 1112033L);
        }
        return total;
    }

    public boolean contains(String value) {
        return this.m_languageTagMode == LanguageTagMode.ABSENT && this.m_minLength <= value.length() && value.length() <= this.m_maxLength && RDFPlainLiteralPatternValueSpaceSubset.s_xsdString.run(value);
    }

    public boolean contains(RDFPlainLiteralDataValue value) {
        String string = value.getString();
        String languageTag = value.getLanguageTag();
        return this.m_languageTagMode == LanguageTagMode.PRESENT && this.m_minLength <= string.length() && string.length() <= this.m_maxLength && RDFPlainLiteralPatternValueSpaceSubset.s_xsdString.run(string) && RDFPlainLiteralPatternValueSpaceSubset.s_languageTag.run(languageTag);
    }

    public void enumerateValues(Collection<Object> values) {
        if (this.m_maxLength == Integer.MAX_VALUE || this.m_languageTagMode == LanguageTagMode.PRESENT) {
            throw new IllegalStateException("Internal error: the data range is infinite!");
        }
        if (this.m_minLength == 0) {
            values.add("");
        }
        char[] temp = new char[this.m_maxLength];
        this.processPosition(temp, values, 0);
    }

    protected void processPosition(char[] temp, Collection<Object> values, int position) {
        if (position < this.m_maxLength) {
            for (int c = 0; c <= 65535; ++c) {
                if (!RDFPlainLiteralLengthInterval.isRDFPlainLiteralCharacter((char)c)) continue;
                temp[position] = (char)c;
                if (this.m_minLength <= position + 1) {
                    values.add(new String(temp, 0, position + 1));
                }
                this.processPosition(temp, values, position + 1);
            }
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append('[');
        buffer.append(this.m_minLength);
        buffer.append("..");
        if (this.m_maxLength == Integer.MAX_VALUE) {
            buffer.append("+INF");
        } else {
            buffer.append(this.m_maxLength);
        }
        buffer.append(']');
        if (this.m_languageTagMode == LanguageTagMode.ABSENT) {
            buffer.append("@<none>");
        } else {
            buffer.append("@<lt>");
        }
        return buffer.toString();
    }

    protected static boolean isIntervalEmpty(int minLength, int maxLength) {
        return minLength > maxLength;
    }

    protected static boolean isRDFPlainLiteralCharacter(char c) {
        return c == '\t' || c == '\n' || c == '\r' || ' ' <= c && c <= '\ud7ff' || '\ue000' <= c && c <= '\ufffd';
    }

    public enum LanguageTagMode {
        PRESENT,
        ABSENT

    }

}

