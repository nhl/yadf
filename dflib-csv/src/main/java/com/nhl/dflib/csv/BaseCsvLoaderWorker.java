package com.nhl.dflib.csv;

import com.nhl.dflib.DataFrame;
import com.nhl.dflib.Index;
import com.nhl.dflib.Series;
import com.nhl.dflib.csv.loader.ColumnBuilder;
import org.apache.commons.csv.CSVRecord;

import java.util.Iterator;

class BaseCsvLoaderWorker implements CsvLoaderWorker {

    protected ColumnBuilder<?>[] columnAccumulators;
    protected Index columnIndex;

    BaseCsvLoaderWorker(Index columnIndex, ColumnBuilder<?>[] columnAccumulators) {
        this.columnIndex = columnIndex;
        this.columnAccumulators = columnAccumulators;
    }

    @Override
    public DataFrame load(Iterator<CSVRecord> it) {
        consumeCSV(it);
        return toDataFrame();
    }

    protected void consumeCSV(Iterator<CSVRecord> it) {
        int width = columnIndex.size();
        while (it.hasNext()) {
            addRow(width, it.next());
        }
    }

    protected DataFrame toDataFrame() {
        int width = columnIndex.size();
        Series<?>[] columns = new Series[width];
        for (int i = 0; i < width; i++) {
            columns[i] = columnAccumulators[i].toColumn();
        }

        return DataFrame.newFrame(columnIndex).columns(columns);
    }

    protected void addRow(int width, CSVRecord row) {
        for (int i = 0; i < width; i++) {
            columnAccumulators[i].add(row);
        }
    }
}
