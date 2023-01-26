package org.hisp.dhis.expression.spi;

import org.hisp.dhis.expression.ast.NamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;

/**
 * Implementation API for all expression languages functions.
 *
 * @author Jan Bernitt
 */
@FunctionalInterface
public interface ExpressionFunctions {

    Object unsupported(String name);

    /**
     * Returns first non-null value (of similar typed values).
     *
     * @param values zero or more values
     * @return the first value that is not null, or null if all values are zero or values is of length zero
     */
    default Object firstNonNull(List<?> values)
    {
        return values.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Returns the largest of the values.
     *
     * @param values zero or more values, nulls allowed (ignored)
     * @return maximum value or null if all values are null
     */
    default Number greatest(List<? extends Number> values)
    {
        return values.stream().filter(Objects::nonNull).max(comparing(Number::doubleValue)).orElse(null);
    }

    /**
     * Returns conditional value based on the condition.
     * Actually not the equivalent of an if-else block but the ternary operator {@code condition ? ifValue : elseValue}.
     *
     * @param condition test, maybe null
     * @param ifValue value when condition is true
     * @param elseValue value when condition if false or null
     * @return either if value or else value based on the condition
     * @param <T> type of value
     */
    default <T> T ifThenElse(Boolean condition, T ifValue, T elseValue)
    {
        return Boolean.TRUE.equals(condition) ? ifValue : elseValue;
    }

    default boolean isNotNull(Object value) {
        return Objects.nonNull(value);
    }

    default boolean isNull(Object value) {
        return Objects.isNull(value);
    }

    /**
     * Returns the smallest of the values
     *
     * @param values zero or more values, nulls allowed (ignored)
     * @return minimum value or null if all values are null
     */
    default Number least(List<? extends Number> values) {
        return values.stream().filter(Objects::nonNull).min(comparing(Number::doubleValue)).orElse(null);
    }

    default double log( Number n ) {
        return Math.log(n.doubleValue());
    }

    default double log10(Number n)
    {
        return Math.log10(n.doubleValue());
    }

    default Number removeZeros(Number n) {
        return n.doubleValue() == 0d ? null : n;
    }

    /*
    Aggregate functions...
     */

    default double avg(double[] values) {
        return AggregateMath.avg(values);
    }
    default double count(double[] values) {
        return AggregateMath.count(values);
    }
    default double max(double[] values) {
        return AggregateMath.max(values);
    }
    default double median(double[] values) {
        return AggregateMath.median(values);
    }
    default double min(double[] values) {
        return AggregateMath.min(values);
    }
    default Double percentileCont(double[] values, Number fraction) {
        return AggregateMath.percentileCont(values, fraction);
    }
    default double stddev(double[] values) {
        return AggregateMath.stddev(values);
    }
    default double stddevPop(double[] values) {
        return AggregateMath.stddevPop(values);
    }
    default double stddevSamp(double[] values) {
        return AggregateMath.stddevSamp(values);
    }
    default double sum(double[] values) {
        return AggregateMath.sum(values);
    }
    default double variance(double[] values) {
        return AggregateMath.variance(values);
    }

    /*
    D2 functions...
     */

    default LocalDate d2_addDays(LocalDate date, Number days) {
        return date == null ? null : days == null ? date : date.plusDays(days.intValue());
    }

    default double d2_ceil(Number value) {
        return value == null ? 0d : Math.ceil(value.doubleValue());
    }

    default String d2_concatenate(Collection<String> values) {
        return String.join("", values);
    }

    @Deprecated // was never in rules engine
    default <T>  T d2_condition(Boolean condition,  T ifValue, T elseValue) {
        return Boolean.TRUE.equals(condition) ? ifValue : elseValue;
    }

    /**
     * Counts the number of values that is entered for the source field in the argument.
     * The source field parameter is the name of one of the defined source fields in the program.
     *
     * @return the number of {@link VariableValue#candidates()}
     */
    default int d2_count(VariableValue value) {
        return value == null ? 0 : value.candidates().size();
    }

    //There is another String d2_count(DataItem); // => SQL

    @Deprecated // was never in rules engine
    default int d2_countIfCondition(Object candidates, String str) {
        return (int) unsupported("d2:countIfCondition");
    }

    default int d2_countIfValue(Object candidates, Object booleanOrNumber) {
        if (candidates == null || booleanOrNumber == null) return 0;
        if (candidates instanceof Collection) return Collections.frequency((Collection<?>) candidates, booleanOrNumber);
        return Objects.equals(candidates, booleanOrNumber) ? 1 : 0;
    }

    default int d2_countIfZeroPos(Object candidates) {
        if (candidates == null) return 0;
        if (candidates instanceof Collection) return (int) ((Collection<?>) candidates).stream()
                .filter(n -> Double.parseDouble(n.toString()) >= 0d).count();
        return Double.parseDouble(candidates.toString()) >= 0d ? 1 : 0;
    }

    default int d2_daysBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end);
    }

    default String d2_extractDataMatrixValue(String gs1Key, String value) {
        return (String) unsupported("d2:extractDataMatrixValue");
    }

    default double d2_floor(Number value) {
        return value == null ? 0d : Math.floor(value.doubleValue());
    }

    default boolean d2_hasUserRole(String role) {
        return (boolean) unsupported("d2:hasUserRole");
    }

    default boolean d2_hasValue(String value) {
        return (boolean) unsupported("d2:hasValue");
    }

    default boolean d2_inOrgUnitGroup(String group) {
        return (boolean) unsupported("d2:inOrgUnitGroup");
    }

    default LocalDate d2_lastEventDate(String value) {
        //FIXME arg is a RuleVariableValue which has a eventDate attached => would require an equivalent to RuleVariableValue here
        // appear that this is maybe more of a modifier on the variable
        return (LocalDate) unsupported("d2:lastEventDate");
    }

    default String d2_left(String input, Integer length) {
        return input == null || length == null ? "" : input.substring(0, Math.min(input.length(), length));
    }

    default int d2_length(String str) {
        return str == null ? 0 : str.length();
    }

    default double d2_maxValue(Object candidates) {
        if (candidates == null) return Double.NaN;
        if (candidates instanceof Collection) return Collections.max((List<Double>)candidates, Double::compare );
        if (candidates instanceof Number) return ((Number) candidates).doubleValue();
        return Double.NaN;
    }

    default int d2_minutesBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.MINUTES.between(start, end);
    }

    default double d2_minValue(Object candidates) {
        if (candidates == null) return Double.NaN;
        if (candidates instanceof Collection) return Collections.min((List<Double>)candidates, Double::compare );
        if (candidates instanceof Number) return ((Number) candidates).doubleValue();
        return Double.NaN;
    }

    default double d2_modulus(Number left, Number right) {
        return (left == null ? 0d : left.doubleValue()) % (right == null ? 0d : right.doubleValue());
    }

    default int d2_monthsBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.MONTHS.between(start, end);
    }

    default double d2_oizp(Number value) {
        return value != null && value.doubleValue() >= 0d ? 1d : 0d;
    }

    @Deprecated // was never in rules engine
    default double d2_relationshipCount(String str) {
        return (double) unsupported("d2:relationshipCount");
    }

    default String d2_right(String input, Integer length) {
        return input == null || length == null ? "" : input.substring(input.length() - length);
    }

    default double d2_round(Number value, Integer precision) {
        if (value == null) return Double.NaN;
        precision = precision == null ? 0 : precision;
        BigDecimal roundedNumber = BigDecimal.valueOf(value.doubleValue()).setScale(precision, RoundingMode.HALF_UP);

        return precision == 0 || roundedNumber.intValue() == roundedNumber.doubleValue()
            ? roundedNumber.intValue()
            : roundedNumber.stripTrailingZeros().doubleValue();
    }

    /**
     * Split the text by delimiter, and keep the nth element(0 is the first).
     */
    default String d2_split(String input, String delimiter, Integer index) {
        if ( input == null || delimiter == null ) return "";
        String[] parts = input.split(Pattern.quote(delimiter));
        return index == null || index < 0 || index >= parts.length ? "" : parts[index];
    }

    default String d2_substring(String input, Integer beginIndex, Integer endIndex) {
        return input == null ? "" : input.substring(beginIndex, endIndex);
    }

    default boolean d2_validatePattern(String input, String regex) {
        return input == null || regex == null ? false : input.matches(regex);
    }

    default int d2_weeksBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.WEEKS.between(start, end);
    }

    default int d2_yearsBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.YEARS.between(start, end);
    }

    /**
     * Evaluates the argument of type number to zero if the value is negative, otherwise to the value itself.
     */
    default Number d2_zing(Number value) {
        if (value != null && value.doubleValue() < 0d)
            return value instanceof Double ? 0d : 0;
        return value;
    }

    /**
     * Returns the number of numeric zero and positive values among the given object arguments. Can be provided with any number of arguments.
     */
    default double d2_zpvc(List<? extends Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).filter(val -> val >= 0d).count();
    }

    default double d2_zScoreHFA(Number parameter, Number weight, String gender) {
        return (double) unsupported("d2:zScoreHFA");
    }

    default double d2_zScoreWFA(Number parameter, Number weight, String gender) {
        return (double) unsupported("d2:zScoreWFA");
    }

    default double d2_zScoreWFH(Number parameter, Number weight, String gender) {
        return (double) unsupported("d2:zScoreWFH");
    }

}
