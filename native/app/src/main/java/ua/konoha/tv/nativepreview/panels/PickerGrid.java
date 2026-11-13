package ua.konoha.tv.nativepreview.panels;

import android.widget.GridLayout;

final class PickerGrid {
    private PickerGrid() {}

    static void reflowColumns(GridLayout grid, int columns) {
        if (grid.getColumnCount() == columns) return;
        // GridLayout materializes automatic positions after measuring. Clear those
        // old positions before shrinking the column count on a live grid.
        for (int i = 0; i < grid.getChildCount(); i++) {
            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) grid.getChildAt(i).getLayoutParams();
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED);
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            grid.getChildAt(i).setLayoutParams(lp);
        }
        grid.setColumnCount(columns);
    }
}
