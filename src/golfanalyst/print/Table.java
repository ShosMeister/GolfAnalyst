package golfanalyst.print;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/*
 +--------------------------------------------------------------------------+
 | Table                                                                    |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The printed table every list report uses: a dark brown header row 30     |
 | units high with white bold 12 headings; body rows of one height,         |
 | alternating white and (248,245,240); text 13 in ink, 8 units in from     |
 | the side for left and right aligned columns; a dark brown rule 2 units   |
 | high under the last row drawn. A SHADED row is (255,240,190) all across  |
 | (the Handicap Report's counting rounds, the Stats Board's best figures   |
 | use single shaded cells), a GROUP row is (240,236,228) and bold (the     |
 | Round Log's "18 holes (13 rounds)"). Text that does not fit its column   |
 | is cut short with "...". A report that breaks a table over pages draws   |
 | the header row again on every page.                                      |
 |                                                                          |
 | Nested types:                                                            |
 | Align  - LEFT, CENTRE or RIGHT                                           |
 | Column - a heading, a width and an alignment                             |
 | Kind   - NORMAL, SHADED or GROUP                                         |
 | Row    - the cells of one row, its kind and the columns to shade alone   |
 |                                                                          |
 | Public Methods:                                                          |
 | add(row), rows(), columns(), width(), rowHeight()                        |
 | drawHeader(sheet, x, y)   - the header row; gives the y under it         |
 | drawRows(sheet, x, y, from, to) - body rows from..to-1; gives the y      |
 | drawEndRule(sheet, x, y)  - the rule under the last row; gives the y     |
 | draw(sheet, x, y, from, to) - all three; gives the y under the rule      |
 +--------------------------------------------------------------------------+
 */
public final class Table
{

    public static final double HEADER_HEIGHT = 30;
    public static final double PAD = 8;
    public static final double END_RULE = 2;
    public static final double HEADER_SIZE = 12;
    public static final double BODY_SIZE = 13;
    public static final Color GROUP_FACE = Sheet.BOX;

    /*
     * Align: where a column's text sits.
     */
    public enum Align
    {
        LEFT, CENTRE, RIGHT
    }

    /*
     * Column: one column: its heading (may be ""), its width in design units and its alignment.
     */
    public record Column(String title, double width, Align align)
    {
    }

    /*
     * Kind: NORMAL rows take the zebra colours by their place, SHADED rows are shaded all across, GROUP rows are tan and bold
     * with their first cell written across the whole row, STRIPE rows always take the zebra colour and WHITE rows are always white.
     */
    public enum Kind
    {
        NORMAL, SHADED, GROUP, STRIPE, WHITE
    }

    /*
     +--------------------------------------------------------------------------+
     | Row                                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One body row.                                                            |
     | Fields: cells - one text per column ("" for an empty cell)               |
     |         kind  - NORMAL, SHADED or GROUP                                  |
     |         shadedCells - columns shaded on their own (may be empty)         |
     +--------------------------------------------------------------------------+
     */
    public record Row(List<String> cells, Kind kind, List<Integer> shadedCells)
    {
        /*
         * Row(cells, kind): a row with no cells shaded on their own.
         */
        public Row(List<String> cells, Kind kind)
        {
            this(cells, kind, List.of());
        }
    }

    private final List<Column> columns;
    private final double rowHeight;
    private final List<Row> rows = new ArrayList<>();

    /*
     * Table(columns, rowHeight): an empty table with these columns and body rows of this height.
     */
    public Table(List<Column> columns, double rowHeight)
    {
        this.columns = List.copyOf(columns);
        this.rowHeight = rowHeight;
    }

    /*
     * add(row): adds a body row at the bottom.
     */
    public void add(Row row)
    {
        rows.add(row);
    }

    /*
     * rows(): the body rows, top to bottom.
     */
    public List<Row> rows()
    {
        return rows;
    }

    /*
     * columns(): the columns, left to right.
     */
    public List<Column> columns()
    {
        return columns;
    }

    /*
     * rowHeight(): the height of a body row.
     */
    public double rowHeight()
    {
        return rowHeight;
    }

    /*
     * width(): the columns' widths added up.
     */
    public double width()
    {
        double total = 0;
        for (Column column : columns)
        {
            total = total + column.width();
        }
        return total;
    }

    /*
     * drawHeader(sheet, x, y): the dark brown header row with white bold headings; gives the y under it.
     */
    public double drawHeader(Sheet sheet, double x, double y)
    {
        sheet.fill(x, y, width(), HEADER_HEIGHT, Sheet.BROWN);
        Font font = Sheet.sans(Font.BOLD, HEADER_SIZE);
        double left = x;
        for (Column column : columns)
        {
            sheet.textIn(column.title(), left, y, column.width(), HEADER_HEIGHT, font, Color.WHITE, column.align(), PAD);
            left = left + column.width();
        }
        return y + HEADER_HEIGHT;
    }

    /*
     +--------------------------------------------------------------------------+
     | drawRows(sheet, x, y, from, to)                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Draws body rows from..to-1. The zebra follows the row's place in the     |
     | whole table, so a table broken over pages keeps its stripes.             |
     |                                                                          |
     | Passed in:  sheet - the page                                             |
     |             x, y  - the top left corner of the first row                 |
     |             from, to - which rows (to not included)                      |
     | Passed out: the y under the last row                                     |
     +--------------------------------------------------------------------------+
     */
    public double drawRows(Sheet sheet, double x, double y, int from, int to)
    {
        double top = y;
        for (int lp = from; lp < to; lp = lp + 1)
        {
            Row row = rows.get(lp);
            Color face = Color.WHITE;
            if (row.kind() == Kind.SHADED)
            {
                face = Sheet.SHADE;
            }
            else if (row.kind() == Kind.GROUP)
            {
                face = GROUP_FACE;
            }
            else if (row.kind() == Kind.STRIPE || (row.kind() == Kind.NORMAL && lp % 2 == 1))
            {
                face = Sheet.ZEBRA;
            }
            sheet.fill(x, top, width(), rowHeight, face);
            if (row.kind() == Kind.GROUP)
            {
                sheet.textIn(row.cells().get(0), x, top, width(), rowHeight, Sheet.sans(Font.BOLD, BODY_SIZE), Sheet.INK, Align.LEFT, PAD);
                top = top + rowHeight;
                continue;
            }
            Font font = Sheet.sans(Font.PLAIN, BODY_SIZE);
            double left = x;
            for (int col = 0; col < columns.size(); col = col + 1)
            {
                Column column = columns.get(col);
                if (row.shadedCells().contains(col))
                {
                    sheet.fill(left, top, column.width(), rowHeight, Sheet.SHADE);
                }
                String words = "";
                if (col < row.cells().size())
                {
                    words = row.cells().get(col);
                }
                sheet.textIn(words, left, top, column.width(), rowHeight, font, Sheet.INK, column.align(), PAD);
                left = left + column.width();
            }
            top = top + rowHeight;
        }
        return top;
    }

    /*
     * drawEndRule(sheet, x, y): the dark brown rule 2 units high under the last row drawn; gives the y under it.
     */
    public double drawEndRule(Sheet sheet, double x, double y)
    {
        sheet.rule(x, y, width(), END_RULE, Sheet.BROWN);
        return y + END_RULE;
    }

    /*
     * draw(sheet, x, y, from, to): the header row, rows from..to-1 and the rule under them; gives the y under the rule.
     */
    public double draw(Sheet sheet, double x, double y, int from, int to)
    {
        double below = drawHeader(sheet, x, y);
        below = drawRows(sheet, x, below, from, to);
        return drawEndRule(sheet, x, below);
    }

    /*
     * rowsThatFit(room): how many body rows fit in a height after the header row and the end rule (at least 0).
     */
    public int rowsThatFit(double room)
    {
        double left = room - HEADER_HEIGHT - END_RULE;
        if (left < rowHeight)
        {
            return 0;
        }
        return (int) Math.floor(left / rowHeight + 1e-9);
    }
}
