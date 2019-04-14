package com.nhl.dflib.csv;

import com.nhl.dflib.DataFrame;
import com.nhl.dflib.Index;
import com.nhl.dflib.map.ValueMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class CsvLoader {

    private int skipRows;
    private Index columns;
    private CSVFormat format;

    // storing converters as list to ensure predictable resolution order when the user supplies overlapping converters
    private List<Pair> builders;

    public CsvLoader() {
        this.format = CSVFormat.DEFAULT;
        this.builders = new ArrayList<>();
    }

    /**
     * Skips the specified number of rows. E.g. if the header is defined manually, you might call this method with "1"
     * as an argument.
     *
     * @param n number of rows to skip
     * @return this loader instance
     */
    public CsvLoader skipRows(int n) {
        this.skipRows = n;
        return this;
    }

    /**
     * Provides an alternative header to the returned DataFrame.
     *
     * @param columns user-defined DataFrame columns
     * @return this loader instance
     */
    public CsvLoader columns(String... columns) {
        this.columns = Index.forLabels(columns);
        return this;
    }

    /**
     * @param typeConverters
     * @return this loader
     * @deprecated since 0.6 as it does not allow to pass primitive values converters. Use per-column type specifiers.
     */
    @Deprecated
    public CsvLoader columnTypes(ValueMapper<String, ?>... typeConverters) {
        for (int i = 0; i < typeConverters.length; i++) {
            int captureI = i;
            builders.add(new Pair(
                    ind -> captureI,
                    new TransformingSeriesBuilder<>(typeConverters[i])));
        }
        return this;
    }


    public CsvLoader columnType(int column, ValueMapper<String, ?> typeConverter) {
        return columnType(column, new TransformingSeriesBuilder<>(typeConverter));
    }

    public CsvLoader columnType(String column, ValueMapper<String, ?> typeConverter) {
        return columnType(column, new TransformingSeriesBuilder<>(typeConverter));
    }

    /**
     * @since 0.6
     */
    public CsvLoader numColumn(int column, Class<? extends Number> type) {
        return columnType(column, numBuilder(type));
    }

    /**
     * @since 0.6
     */
    public CsvLoader numColumn(String column, Class<? extends Number> type) {
        return columnType(column, numBuilder(type));
    }

    private CsvLoader columnType(int column, SeriesBuilder<?> columnBuilder) {
        builders.add(new Pair(i -> column, columnBuilder));
        return this;
    }

    private CsvLoader columnType(String column, SeriesBuilder<?> columnBuilder) {
        builders.add(new Pair(i -> i.position(column), columnBuilder));
        return this;
    }

    private SeriesBuilder<?> numBuilder(Class<? extends Number> type) {

        if (Integer.class.equals(type)) {
            return new IntSeriesBuilder();
        }

        // TODO: handle other primitive types as such
        if (Long.class.equals(type)) {
            return new TransformingSeriesBuilder<>(ValueMapper.stringToLong());
        }

        if (Double.class.equals(type)) {
            return new TransformingSeriesBuilder<>(ValueMapper.stringToDouble());
        }

        if (Float.class.equals(type)) {
            return new TransformingSeriesBuilder<>(ValueMapper.stringToFloat());
        }

        if (BigDecimal.class.equals(type)) {
            return new TransformingSeriesBuilder<>(ValueMapper.stringToBigDecimal());
        }

        if (BigInteger.class.equals(type)) {
            return new TransformingSeriesBuilder<>(ValueMapper.stringToBigInteger());
        }

        throw new IllegalArgumentException("Can't map numeric type to a string converter: " + type);
    }

    /**
     * @since 0.6
     */
    public CsvLoader dateColumn(int column) {
        return columnType(column, ValueMapper.stringToDate());
    }

    /**
     * @since 0.6
     */
    public CsvLoader dateColumn(String column) {
        return columnType(column, ValueMapper.stringToDate());
    }

    /**
     * @since 0.6
     */
    public CsvLoader dateTimeColumn(int column) {
        return columnType(column, ValueMapper.stringToDateTime());
    }

    /**
     * @since 0.6
     */
    public CsvLoader dateTimeColumn(String column) {
        return columnType(column, ValueMapper.stringToDateTime());
    }

    /**
     * @since 0.6
     */
    public CsvLoader dateColumn(int column, DateTimeFormatter formatter) {
        return columnType(column, ValueMapper.stringToDate(formatter));
    }

    /**
     * @since 0.6
     */
    public CsvLoader dateColumn(String column, DateTimeFormatter formatter) {
        return columnType(column, ValueMapper.stringToDate(formatter));
    }

    /**
     * @since 0.6
     */
    public CsvLoader dateTimeColumn(int column, DateTimeFormatter formatter) {
        return columnType(column, ValueMapper.stringToDateTime(formatter));
    }

    /**
     * @since 0.6
     */
    public CsvLoader dateTimeColumn(String column, DateTimeFormatter formatter) {
        return columnType(column, ValueMapper.stringToDateTime(formatter));
    }

    /**
     * Optionally sets the style or format of the imported CSV. CSVFormat comes from "commons-csv" library and
     * contains a number of predefined formats, such as CSVFormat.MYSQL, etc. It also allows to customize the format
     * further, by defining custom delimiters, line separators, etc.
     *
     * @param format a format object defined in commons-csv library
     * @return this loader instance
     */
    public CsvLoader format(CSVFormat format) {
        this.format = format;
        return this;
    }

    public DataFrame load(File file) {
        try (Reader r = new FileReader(file)) {
            return load(r);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + file, e);
        }
    }

    public DataFrame load(String filePath) {
        try (Reader r = new FileReader(filePath)) {
            return load(r);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + filePath, e);
        }
    }

    public DataFrame load(Reader reader) {
        try {
            Iterator<CSVRecord> it = format.parse(reader).iterator();

            rewind(it);
            Index columns = createColumns(it);

            if (!it.hasNext()) {
                return DataFrame.forRows(columns, Collections.emptyList());
            }

            SeriesBuilder<?>[] builders = createSeriesBuilders(columns);
            return new CsvLoaderWorker(columns, builders).load(it);

        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV", e);
        }
    }

    private void rewind(Iterator<CSVRecord> it) {
        for (int i = 0; i < skipRows && it.hasNext(); i++) {
            it.next();
        }
    }

    private Index createColumns(Iterator<CSVRecord> it) {
        if (it.hasNext()) {
            return columns != null ? columns : loadColumns(it.next());
        } else {
            return columns != null ? columns : Index.forLabels();
        }
    }

    private Index loadColumns(CSVRecord header) {

        int width = header.size();
        String[] columnNames = new String[width];
        for (int i = 0; i < width; i++) {
            columnNames[i] = header.get(i);
        }

        return Index.forLabels(columnNames);
    }

    private SeriesBuilder<?>[] createSeriesBuilders(Index columns) {

        int w = columns.size();
        SeriesBuilder<?>[] builders = new SeriesBuilder[w];

        // there may be overlapping pairs... the last one wins
        for (Pair p : this.builders) {
            builders[p.positionResolver.apply(columns)] = p.builder;
        }

        // fill missing builders with no-transform builders
        for (int i = 0; i < w; i++) {
            if (builders[i] == null) {
                builders[i] = new NoTransformSeriesBuilder();
            }
        }

        return builders;
    }

    private class Pair {
        Function<Index, Integer> positionResolver;
        SeriesBuilder<?> builder;

        Pair(Function<Index, Integer> positionResolver, SeriesBuilder<?> builder) {
            this.positionResolver = positionResolver;
            this.builder = builder;
        }
    }
}
