package am.gortsup.testing.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Date and time computation utilities including business days, leap years, and interval comparisons.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // Utility class
    }

    /**
     * Determines whether a given year is a leap year in the Gregorian calendar.
     * Rules:
     * - Year must be divisible by 4
     * - If divisible by 100, must also be divisible by 400
     *
     * @param year the year (e.g., 2024, 2000, 1900)
     * @return true if leap year, false otherwise
     */
    public static boolean isLeapYear(int year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 100 == 0) {
            return year % 400 == 0;
        }
        return true;
    }

    /**
     * Calculates the total number of days between two dates (inclusive of start, exclusive of end).
     * If start is after end, returns negative days.
     *
     * @param start start date (non-null)
     * @param end end date (non-null)
     * @return number of days between
     * @throws IllegalArgumentException if either date is null
     */
    public static long calculateDaysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Determines whether a given date falls on a weekend (Saturday or Sunday).
     *
     * @param date the date to check
     * @return true if weekend, false if Monday-Friday
     * @throws IllegalArgumentException if date is null
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /**
     * Calculates the number of business days (Monday through Friday) between two dates.
     * Starts counting from 'start' (inclusive) up to 'end' (exclusive).
     * If start and end are equal, returns 0.
     * If start is after end, returns 0.
     *
     * @param start start date
     * @param end end date
     * @return number of business days
     * @throws IllegalArgumentException if either date is null
     */
    public static int calculateBusinessDaysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        if (!start.isBefore(end)) {
            return 0;
        }

        int businessDays = 0;
        LocalDate current = start;
        while (current.isBefore(end)) {
            if (!isWeekend(current)) {
                businessDays++;
            }
            current = current.plusDays(1);
        }
        return businessDays;
    }

    /**
     * Adds a given number of business days (skipping Saturday and Sunday) to a starting date.
     *
     * @param start start date
     * @param days number of business days to add (must be >= 0)
     * @return resulting LocalDate after adding business days
     * @throws IllegalArgumentException if start is null or days is negative
     */
    public static LocalDate addBusinessDays(LocalDate start, int days) {
        if (start == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (days < 0) {
            throw new IllegalArgumentException("Days to add cannot be negative");
        }

        LocalDate result = start;
        int added = 0;
        while (added < days) {
            result = result.plusDays(1);
            if (!isWeekend(result)) {
                added++;
            }
        }
        return result;
    }

    /**
     * Determines whether two closed date intervals [start1, end1] and [start2, end2] overlap.
     *
     * @param start1 start of first interval
     * @param end1 end of first interval (must be >= start1)
     * @param start2 start of second interval
     * @param end2 end of second interval (must be >= start2)
     * @return true if intervals overlap, false otherwise
     * @throws IllegalArgumentException if any date is null or start is after end in either interval
     */
    public static boolean doIntervalsOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        if (start1.isAfter(end1) || start2.isAfter(end2)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    /**
     * Checks if a target date is within a given date range [min, max] (inclusive).
     *
     * @param date target date
     * @param min range start
     * @param max range end (must be >= min)
     * @return true if target is between min and max inclusive
     * @throws IllegalArgumentException if any argument is null or min is after max
     */
    public static boolean isDateInRange(LocalDate date, LocalDate min, LocalDate max) {
        if (date == null || min == null || max == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        if (min.isAfter(max)) {
            throw new IllegalArgumentException("Min date cannot be after max date");
        }
        return !date.isBefore(min) && !date.isAfter(max);
    }
}
