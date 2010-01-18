package org.pillarone.riskanalytics.core.parameterization;

import org.pillarone.riskanalytics.core.parameterization.AbstractMultiDimensionalParameter;

import java.util.List;

public class SimpleMultiDimensionalParameter extends AbstractMultiDimensionalParameter {

    public SimpleMultiDimensionalParameter(List cellValues) {
        super(cellValues);
    }

    public SimpleMultiDimensionalParameter(String cellValues) {
        super(cellValues);
    }

    public int getTitleColumnCount() {
        return 0;
    }

    public int getTitleRowCount() {
        return 0;
    }

    protected void rowsAdded(int i) {

    }

    protected void columnsAdded(int i) {

    }

    protected void rowsRemoved(int i) {

    }

    protected void columnsRemoved(int i) {

    }

    public boolean isCellEditable(int row, int column) {
        return true;
    }

    protected void appendAdditionalConstructorArguments(StringBuffer buffer) {

    }

    public boolean columnCountChangeable() {
        return true;
    }

    public boolean rowCountChangeable() {
        return true;
    }
}