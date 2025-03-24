package org.semanticweb.HermiT.datatypes.datetime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTime {
    private static final String TWO_DIGITS = "([0-9]{2})";
    public static final int NO_TIMEZONE = Integer.MAX_VALUE;
    public static final long MAX_TIME_ZONE_CORRECTION = 50400000L;
    protected static final Pattern s_dateTimePattern = Pattern.compile("(-?[0-9]{4,})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})([.]([0-9]{1,3}))?((Z)|(([+]|-)([0-9]{2}):([0-9]{2})))?");
    protected static final int YEAR_GROUP = 1;
    protected static final int MONTH_GROUP = 2;
    protected static final int DAY_GROUP = 3;
    protected static final int HOUR_GROUP = 4;
    protected static final int MINUTE_GROUP = 5;
    protected static final int SECOND_WHOLE_GROUP = 6;
    protected static final int SECOND_FRACTION_GROUP = 8;
    protected static final int TZ_OFFSET_GROUP = 9;
    protected static final int TZ_OFFSET_Z_GROUP = 10;
    protected static final int TZ_OFFSET_SIGN_GROUP = 12;
    protected static final int TZ_OFFSET_HOUR_GROUP = 13;
    protected static final int TZ_OFFSET_MINUTE_GROUP = 14;
    protected final long m_timeOnTimeline;
    protected final boolean m_lastDayInstant;
    protected final int m_timeZoneOffset;

    public DateTime(int year, int month, int day, int hour, int minute, int second, int millisecond, int timeZoneOffset) {
        this.m_timeOnTimeline = this.getTimeOnTimelineRaw(year, month, day, hour, minute, second, millisecond) - (timeZoneOffset == Integer.MAX_VALUE ? 0L : (long)timeZoneOffset * 60L * 1000L);
        this.m_lastDayInstant = hour == 24 && minute == 0 && second == 0 && millisecond == 0;
        this.m_timeZoneOffset = timeZoneOffset;
    }

    public DateTime(long timeOnTimeline, boolean lastDayInstant, int timeZoneOffset) {
        this.m_timeOnTimeline = timeOnTimeline;
        this.m_lastDayInstant = lastDayInstant;
        this.m_timeZoneOffset = timeZoneOffset;
    }

    public String toString() {
        long timeOnTimeline = this.m_timeOnTimeline;
        if (this.m_timeZoneOffset != Integer.MAX_VALUE) {
            timeOnTimeline += (long)this.m_timeZoneOffset * 60L * 1000L;
        }
        int timePart = (int)(timeOnTimeline % 86400000L);
        long days = timeOnTimeline / 86400000L;
        if (timePart < 0) {
            --days;
            assert ((timePart += 86400000) >= 0);
        }
        int millisecond = timePart % 1000;
        int second = (timePart /= 1000) % 60;
        int minute = (int)((long)(timePart /= 60) % 60L);
        int hour = (int)((long)(timePart /= 60) % 24L);
        int year = (int)(days / 367L);
        if (year >= 0) {
            while (days >= DateTime.daysToYearStart(year + 1)) {
                ++year;
            }
            days -= DateTime.daysToYearStart(year);
        } else {
            while (days < DateTime.daysToYearStart(year - 1)) {
                --year;
            }
            days -= DateTime.daysToYearStart(--year);
        }
        int month = 1;
        int daysInMonth = DateTime.daysInMonth(year, month);
        while (days > (long)daysInMonth) {
            days -= daysInMonth;
            daysInMonth = DateTime.daysInMonth(year, ++month);
        }
        int day = (int)days + 1;
        if (day == 0) {
            if (--month == 0) {
                month = 12;
                --year;
            }
            day = DateTime.daysInMonth(year, month);
        }
        if (this.m_lastDayInstant) {
            assert (hour == 0 && minute == 0 && second == 0 && millisecond == 0);
            hour = 24;
            if (--day <= 0) {
                if (--month <= 0) {
                    month = 12;
                    --year;
                }
                day = DateTime.daysInMonth(year, month);
            }
        }
        StringBuffer buffer = new StringBuffer();
        this.appendPadded(buffer, year, 4);
        buffer.append('-');
        this.appendPadded(buffer, month, 2);
        buffer.append('-');
        this.appendPadded(buffer, day, 2);
        buffer.append('T');
        this.appendPadded(buffer, hour, 2);
        buffer.append(':');
        this.appendPadded(buffer, minute, 2);
        buffer.append(':');
        this.appendPadded(buffer, second, 2);
        if (millisecond > 0) {
            buffer.append('.');
            this.appendPadded(buffer, millisecond, 3);
        }
        if (this.m_timeZoneOffset != Integer.MAX_VALUE) {
            if (this.m_timeZoneOffset == 0) {
                buffer.append('Z');
            } else {
                int absTimeZoneOffset;
                if (this.m_timeZoneOffset > 0) {
                    buffer.append('+');
                    absTimeZoneOffset = this.m_timeZoneOffset;
                } else {
                    buffer.append('-');
                    absTimeZoneOffset = - this.m_timeZoneOffset;
                }
                int timeZoneHour = absTimeZoneOffset / 60;
                int timeZoneMinute = absTimeZoneOffset % 60;
                this.appendPadded(buffer, timeZoneHour, 2);
                buffer.append(':');
                this.appendPadded(buffer, timeZoneMinute, 2);
            }
        }
        return buffer.toString();
    }

    public long getTimeOnTimeline() {
        return this.m_timeOnTimeline;
    }

    public boolean hasTimeZoneOffset() {
        return this.m_timeZoneOffset != Integer.MAX_VALUE;
    }

    public int getTimeZoneOffset() {
        return this.m_timeZoneOffset;
    }

    protected void appendPadded(StringBuffer buffer, int value, int digits) {
        if (value < 0) {
            buffer.append('-');
        }
        String stringAbsValue = String.valueOf(Math.abs(value));
        for (int i = digits - stringAbsValue.length(); i > 0; --i) {
            buffer.append('0');
        }
        buffer.append(stringAbsValue);
    }

    public static DateTime parse(String lexicalForm) {
        Matcher matcher = s_dateTimePattern.matcher(lexicalForm.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            int millisecond;
            int timeZoneOffset;
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            int hour = Integer.parseInt(matcher.group(4));
            int minute = Integer.parseInt(matcher.group(5));
            int second = Integer.parseInt(matcher.group(6));
            String millisecondString = matcher.group(8);
            if (millisecondString != null) {
                while (millisecondString.length() < 3) {
                    millisecondString = millisecondString + '0';
                }
                millisecond = Integer.parseInt(millisecondString);
            } else {
                millisecond = 0;
            }
            if (year < -9999 || year > 9999 || month <= 0 || month > 12 || day <= 0 || day > DateTime.daysInMonth(year, month) || hour < 0 || hour > 24 || hour == 24 && (minute != 0 || second != 0 || millisecond != 0) || minute < 0 || minute >= 60 || second < 0 || second >= 60 || millisecond < 0 || millisecond >= 1000) {
                return null;
            }
            if (matcher.group(9) == null) {
                timeZoneOffset = Integer.MAX_VALUE;
            } else if (matcher.group(10) != null) {
                timeZoneOffset = 0;
            } else {
                int sign = "-".equals(matcher.group(12)) ? -1 : 1;
                int timeZoneOffsetHour = Integer.parseInt(matcher.group(13));
                int timeZoneOffsetMinute = Integer.parseInt(matcher.group(14));
                if (timeZoneOffsetHour < 0 || timeZoneOffsetHour > 14 || timeZoneOffsetHour == 14 && timeZoneOffsetMinute != 0 || timeZoneOffsetMinute < 0 || timeZoneOffsetMinute >= 60) {
                    return null;
                }
                timeZoneOffset = sign * (timeZoneOffsetHour * 60 + timeZoneOffsetMinute);
            }
            return new DateTime(year, month, day, hour, minute, second, millisecond, timeZoneOffset);
        }
        catch (NumberFormatException nfe) {
            return null;
        }
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof DateTime)) {
            return false;
        }
        DateTime thatObject = (DateTime)that;
        return this.m_timeOnTimeline == thatObject.m_timeOnTimeline && this.m_lastDayInstant == thatObject.m_lastDayInstant && this.m_timeZoneOffset == thatObject.m_timeZoneOffset;
    }

    public int hashCode() {
        return (int)(this.m_timeOnTimeline * 3L + (long)this.m_timeZoneOffset + (this.m_lastDayInstant ? 117L : 0L));
    }

    protected long getTimeOnTimelineRaw(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        long yearMinusOne = year - 1;
        long timeOnTimeline = 31536000L * yearMinusOne;
        timeOnTimeline += 86400L * (yearMinusOne / 400L - yearMinusOne / 100L + yearMinusOne / 4L);
        for (int monthIndex = 1; monthIndex < month; ++monthIndex) {
            timeOnTimeline += 86400L * (long)DateTime.daysInMonth(year, monthIndex);
        }
        timeOnTimeline += 86400L * (long)(day - 1);
        timeOnTimeline += 3600L * (long)hour + 60L * (long)minute + (long)second;
        timeOnTimeline = timeOnTimeline * 1000L + (long)millisecond;
        return timeOnTimeline;
    }

    protected static long daysToYearStart(int year) {
        long yearMinusOne = year - 1;
        return 365L * yearMinusOne + yearMinusOne / 400L - yearMinusOne / 100L + yearMinusOne / 4L;
    }

    protected static int daysInMonth(int year, int month) {
        if (month == 2) {
            if (year % 4 != 0 || year % 100 == 0 && year % 400 != 0) {
                return 28;
            }
            return 29;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        return 31;
    }

    public static boolean isLastDayInstant(long timeOnTimeline) {
        return timeOnTimeline % 86400000L == 0L;
    }

    public static boolean secondsAreZero(long timeOnTimeline) {
        return timeOnTimeline % 60000L == 0L;
    }

    public static int getMinutesInDay(long timeOnTimeline) {
        return (int)(timeOnTimeline / 60000L % 1440L);
    }
}

