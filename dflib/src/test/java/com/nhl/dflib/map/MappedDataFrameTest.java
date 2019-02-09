package com.nhl.dflib.map;

import com.nhl.dflib.DFAsserts;
import com.nhl.dflib.DataFrame;
import com.nhl.dflib.Index;
import org.junit.Test;

import static org.junit.Assert.*;

public class MappedDataFrameTest {
    
    @Test
    public void testIterator() {

        Index i = Index.withNames("a", "b");

        MappedDataFrame df = new MappedDataFrame(
                i,
                DataFrame.fromRows(i, DataFrame.row("one", 1), DataFrame.row("two", 2)),
                RowMapper.copyMapper());

        new DFAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, "one", 1)
                .expectRow(1, "two", 2);
    }

    @Test
    public void testHead() {

        Index columns = Index.withNames("a", "b");

        DataFrame df = new MappedDataFrame(
                columns,
                DataFrame.fromRows(columns, DataFrame.row("one", 1), DataFrame.row("two", 2), DataFrame.row("three", 3)),
                RowMapper.copyMapper()).head(2);

        new DFAsserts(df, columns)
                .expectHeight(2)
                .expectRow(0, "one", 1)
                .expectRow(1, "two", 2);
    }

    @Test
    public void testRenameColumn() {
        Index i = Index.withNames("a", "b");

        DataFrame df = new MappedDataFrame(
                i,
                DataFrame.fromRows(i, DataFrame.row("one", 1), DataFrame.row("two", 2)),
                RowMapper.copyMapper()).renameColumn("b", "c");

        new DFAsserts(df, "a", "c")
                .expectHeight(2)
                .expectRow(0, "one", 1)
                .expectRow(1, "two", 2);
    }

    @Test
    public void testMapColumn() {

        Index i = Index.withNames("a", "b");

        DataFrame df = new MappedDataFrame(
                i,
                DataFrame.fromRows(i, DataFrame.row("one", 1), DataFrame.row("two", 2)),
                RowMapper.copyMapper()).mapColumn("b", (c, r) -> c.get(r, 1).toString());

        new DFAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, "one", "1")
                .expectRow(1, "two", "2");
    }

    @Test
    public void testMap() {

        Index i = Index.withNames("a", "b");

        DataFrame df = new MappedDataFrame(
                i,
                DataFrame.fromRows(i, DataFrame.row("one", 1), DataFrame.row("two", 2)),
                RowMapper.copyMapper())
                .map(i, RowMapper.columnMapper("a", (cx, rx) -> cx.get(rx, 0) + "_"));

        new DFAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, "one_", 1)
                .expectRow(1, "two_", 2);
    }

    @Test
    public void testMap_ChangeRowStructure() {

        Index i = Index.withNames("a", "b");
        Index i1 = Index.withNames("c", "d", "e");

        DataFrame df = new MappedDataFrame(
                i,
                DataFrame.fromRows(i, DataFrame.row("one", 1), DataFrame.row("two", 2)),
                RowMapper.copyMapper())
                .map(i1, (c, sr, tr) -> c
                        .set(tr, 0, sr[0])
                        .set(tr, 1, ((int) sr[1]) * 10)
                        .set(tr, 2, sr[1]));

        new DFAsserts(df, i1)
                .expectHeight(2)
                .expectRow(0, "one", 10, 1)
                .expectRow(1, "two", 20, 2);
    }

    @Test
    public void testMap_ChangeRowStructure_Chained() {

        Index i = Index.withNames("a", "b");
        Index i1 = Index.withNames("c", "d", "e");
        Index i2 = Index.withNames("f", "g");

        DataFrame df = new MappedDataFrame(
                i,
                DataFrame.fromRows(i, DataFrame.row("one", 1), DataFrame.row("two", 2)),
                RowMapper.copyMapper())
                .map(i1, (c, sr, tr) -> c
                        .set(tr, 0, sr[0])
                        .set(tr, 1, ((int) sr[1]) * 10)
                        .set(tr, 2, sr[1]))
                .map(i2, (c, sr, tr) -> c
                        .set(tr, 0, sr[0])
                        .set(tr, 1, sr[1]));

        new DFAsserts(df, i2)
                .expectHeight(2)
                .expectRow(0, "one", 10)
                .expectRow(1, "two", 20);
    }

    @Test
    public void testMap_ChangeRowStructure_EmptyDF() {

        Index i = Index.withNames("a", "b");
        Index i1 = Index.withNames("c", "d", "e");

        DataFrame df = new MappedDataFrame(i, DataFrame.fromRows(i), RowMapper.copyMapper())
                .map(i1, (c, sr, tr) -> c
                        .set(tr, 0, sr[0])
                        .set(tr, 1, ((int) sr[1]) * 10)
                        .set(tr, 2, sr[1]));

        assertSame(i1, df.getColumns());

        new DFAsserts(df, i1).expectHeight(0);
    }

    @Test
    public void testToString() {
        Index i = Index.withNames("a", "b");
        DataFrame df = new MappedDataFrame(i, DataFrame.fromRows(i,
                DataFrame.row("one", 1),
                DataFrame.row("two", 2),
                DataFrame.row("three", 3),
                DataFrame.row("four", 4)), RowMapper.copyMapper());

        assertEquals("MappedDataFrame [{a:one,b:1},{a:two,b:2},{a:three,b:3},...]", df.toString());
    }

    @Test
    public void testZip() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new MappedDataFrame(i1, DataFrame.fromRows(i1,
                DataFrame.row(1),
                DataFrame.row(2)), RowMapper.copyMapper());

        Index i2 = Index.withNames("b");
        DataFrame df2 = new MappedDataFrame(i2, DataFrame.fromRows(i2,
                DataFrame.row(10),
                DataFrame.row(20)), RowMapper.copyMapper());

        DataFrame df = df1.hConcat(df2);
        new DFAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, 1, 10)
                .expectRow(1, 2, 20);
    }

    @Test
    public void testZip_Self() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new MappedDataFrame(i1, DataFrame.fromRows(i1,
                DataFrame.row(1),
                DataFrame.row(2)), RowMapper.copyMapper());

        DataFrame df = df1.hConcat(df1);

        new DFAsserts(df, "a", "a_")
                .expectHeight(2)
                .expectRow(0, 1, 1)
                .expectRow(1, 2, 2);
    }

    @Test
    public void testZip_LeftIsShorter() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new MappedDataFrame(i1, DataFrame.fromRows(i1, DataFrame.row(2)), RowMapper.copyMapper());

        Index i2 = Index.withNames("b");
        DataFrame df2 = new MappedDataFrame(i2, DataFrame.fromRows(i2,
                DataFrame.row(10),
                DataFrame.row(20)), RowMapper.copyMapper());

        DataFrame df = df1.hConcat(df2);
        new DFAsserts(df, "a", "b")
                .expectHeight(1)
                .expectRow(0, 2, 10);
    }

    @Test
    public void testZip_RightIsShorter() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new MappedDataFrame(i1, DataFrame.fromRows(i1,
                DataFrame.row(2)), RowMapper.copyMapper());

        Index i2 = Index.withNames("b");
        DataFrame df2 = new MappedDataFrame(i2, DataFrame.fromRows(i2,
                DataFrame.row(10),
                DataFrame.row(20)), RowMapper.copyMapper());

        DataFrame df = df2.hConcat(df1);
        new DFAsserts(df, "b", "a")
                .expectHeight(1)
                .expectRow(0, 10, 2);
    }

    @Test
    public void testFilter() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new MappedDataFrame(i1, DataFrame.fromRows(i1,
                DataFrame.row(10),
                DataFrame.row(20)), RowMapper.copyMapper());

        DataFrame df = df1.filter((c, r) -> ((int) c.get(r, 0)) > 15);
        new DFAsserts(df, "a")
                .expectHeight(1)
                .expectRow(0, 20);
    }

    @Test
    public void testFilterColumn_Name() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new MappedDataFrame(i1, DataFrame.fromRows(i1,
                DataFrame.row(10),
                DataFrame.row(20)), RowMapper.copyMapper());

        DataFrame df = df1.filterByColumn("a", (Integer v) -> v > 15);
        new DFAsserts(df, "a")
                .expectHeight(1)
                .expectRow(0, 20);
    }

    @Test
    public void testFilterColumn_Pos() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new MappedDataFrame(i1, DataFrame.fromRows(i1,
                DataFrame.row(10),
                DataFrame.row(20)), RowMapper.copyMapper());

        DataFrame df = df1.filterByColumn(0, (Integer v) -> v > 15);
        new DFAsserts(df, "a")
                .expectHeight(1)
                .expectRow(0, 20);
    }
}
