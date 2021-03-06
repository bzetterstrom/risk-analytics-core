package org.pillarone.riskanalytics.core.simulation.item.parameter

import org.pillarone.riskanalytics.core.parameter.Parameter
import org.pillarone.riskanalytics.core.parameterization.AbstractMultiDimensionalParameter
import org.pillarone.riskanalytics.core.parameter.MultiDimensionalParameter
import org.pillarone.riskanalytics.core.parameterization.ConstrainedMultiDimensionalParameter
import org.pillarone.riskanalytics.core.parameterization.ComboBoxTableMultiDimensionalParameter
import org.joda.time.DateTime
import org.pillarone.riskanalytics.core.parameterization.IMultiDimensionalConstraints

class MultiDimensionalParameterHolder extends ParameterHolder implements IMarkerValueAccessor {

    private AbstractMultiDimensionalParameter value;

    public MultiDimensionalParameterHolder(Parameter parameter) {
        super(parameter.path, parameter.periodIndex);
        this.value = parameter.parameterInstance
    }

    public MultiDimensionalParameterHolder(String path, int periodIndex, AbstractMultiDimensionalParameter value) {
        super(path, periodIndex);
        this.value = value;
    }

    AbstractMultiDimensionalParameter getBusinessObject() {
        return value;
    }

    void applyToDomainObject(Parameter parameter) {
        parameter.parameterInstance = value
    }

    Parameter createEmptyParameter() {
        return new MultiDimensionalParameter(path: path, periodIndex: periodIndex)
    }

    protected void updateValue(Object newValue) {
        value = newValue
    }

    public MultiDimensionalParameterHolder clone() {
        MultiDimensionalParameterHolder holder = (MultiDimensionalParameterHolder) super.clone();
        holder.@value = (AbstractMultiDimensionalParameter) value.clone()
        return holder
    }

    List<String> referencePaths(Class markerInterface, String refValue) {
        List<String> paths = new ArrayList()
        if ((value instanceof ConstrainedMultiDimensionalParameter)
                && ((ConstrainedMultiDimensionalParameter) value).referencePaths(markerInterface, refValue)) {
            paths.add(path)
        }
        else if ((value instanceof ComboBoxTableMultiDimensionalParameter) && markerInterface.is(value.markerClass)) {
            if (value.values.indexOf(refValue) > -1) {
                paths.add(path)
            }
        }
        return paths
    }

    List<String> updateReferenceValues(Class markerInterface, String oldValue, String newValue) {
        List<String> referencePaths = referencePaths(markerInterface, oldValue)
        if (referencePaths) {
            if (value instanceof ConstrainedMultiDimensionalParameter) {
                ((ConstrainedMultiDimensionalParameter) value).updateReferenceValues(markerInterface, oldValue, newValue)
            }
            else if (value instanceof ComboBoxTableMultiDimensionalParameter) {
                int row = value.values.indexOf(oldValue)
                row += value.getTitleRowCount()
                if (row > -1) {
                    value.setValueAt newValue, row, 0
                }
            }
        }
        return referencePaths
    }

    @Override
    void clearCachedValues() {
        if (value != null) {
            value.simulationModel = null
        }
    }


}
