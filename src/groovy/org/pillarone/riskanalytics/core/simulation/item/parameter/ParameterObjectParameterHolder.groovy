package org.pillarone.riskanalytics.core.simulation.item.parameter

import org.pillarone.riskanalytics.core.parameter.EnumParameter
import org.pillarone.riskanalytics.core.parameter.Parameter
import org.pillarone.riskanalytics.core.parameter.ParameterEntry
import org.pillarone.riskanalytics.core.parameter.ParameterObjectParameter
import org.pillarone.riskanalytics.core.parameterization.IParameterObjectClassifier
import org.pillarone.riskanalytics.core.parameterization.IParameterObject
import org.pillarone.riskanalytics.core.parameterization.AbstractMultiDimensionalParameter

class ParameterObjectParameterHolder extends ParameterHolder implements IMarkerValueAccessor {

    //Sufficient for the UI (faster to instantiate)
    IParameterObjectClassifier classifier
    Map<String, ParameterHolder> classifierParameters

    public ParameterObjectParameterHolder(Parameter parameter) {
        super(parameter.path, parameter.periodIndex);
        classifierParameters = new HashMap<String, ParameterHolder>()
        this.classifier = Thread.currentThread().contextClassLoader.loadClass(parameter.type.parameterType).valueOf(parameter.type.parameterValue)
        for (ParameterEntry entry in parameter.parameterEntries) {
            def holder = ParameterHolderFactory.getHolder(entry.parameterEntryValue)
            classifierParameters.put(entry.parameterEntryKey, holder)
        }
    }

    public ParameterObjectParameterHolder(String path, int periodIndex, IParameterObject value) {
        super(path, periodIndex);
        classifierParameters = new HashMap<String, ParameterHolder>()
        this.classifier = value.type
        for (Map.Entry entry in value.parameters) {
            classifierParameters.put(entry.key, ParameterHolderFactory.getHolder(path + ":$entry.key", periodIndex, entry.value))
        }
    }

    IParameterObject getBusinessObject() {
        return classifier.getParameterObject(getParameterMap())
    }

    Map getParameterMap() {
        Map result = [:]
        for (Map.Entry entry in classifierParameters) {
            result.put(entry.key, entry.value.getBusinessObject())
        }
        return result
    }

    void applyToDomainObject(Parameter parameter) {
        parameter.type.parameterValue = classifier.toString()
        List<ParameterEntry> usableEntries = parameter.parameterEntries != null ? parameter.parameterEntries.toList() : []
        for (Map.Entry<String, ParameterHolder> entry in classifierParameters) {
            ParameterEntry currentEntry = null
            if (!usableEntries.empty) {
                currentEntry = usableEntries.remove(0 as int)
                currentEntry.parameterEntryValue.delete()
            } else {
                currentEntry = new ParameterEntry()
                parameter.addToParameterEntries(currentEntry)
            }
            currentEntry.parameterEntryKey = entry.key
            currentEntry.parameterEntryValue = entry.value.createEmptyParameter()
            entry.value.applyToDomainObject(currentEntry.parameterEntryValue)
        }
        for (ParameterEntry unusedEntry in usableEntries) {
            parameter.removeFromParameterEntries(unusedEntry)
            unusedEntry.delete()
        }
    }


    Parameter createEmptyParameter() {
        ParameterObjectParameter parameter = new ParameterObjectParameter(path: path, periodIndex: periodIndex)
        parameter.type = new EnumParameter(path: path + ":type", periodIndex: periodIndex, parameterType: classifier.getClass().name)
        return parameter
    }

    protected void updateValue(Object newValue) {
        classifier = classifier.valueOf(newValue)
        Map<String, ParameterHolder> newClassifierParameters = new HashMap<String, ParameterHolder>()
        for (Map.Entry entry in classifier.parameters) {
            ParameterHolder holder = null
            ParameterHolder oldEntry = classifierParameters.get(entry.key)
            if (oldEntry != null && oldEntry.businessObject.class.name == entry.value.class.name) {
                holder = classifierParameters.get(entry.key)
            } else {
                holder = ParameterHolderFactory.getHolder(path + ":$entry.key", periodIndex, entry.value)
            }
            newClassifierParameters.put(entry.key, holder)
        }
        classifierParameters = newClassifierParameters
    }

    boolean hasParameterChanged() {
        if (added || removed) return false

        boolean result = modified
        if (!result) {
            result = classifierParameters.values().any { it.hasParameterChanged() }
        }
        return result
    }

    public ParameterObjectParameterHolder clone() {
        ParameterObjectParameterHolder holder = (ParameterObjectParameterHolder) super.clone();
        holder.classifier = classifier
        holder.classifierParameters = new HashMap<String, ParameterHolder>()
        for (Map.Entry entry in classifierParameters) {
            holder.classifierParameters.put(entry.key, entry.value.clone())
        }
        return holder
    }

    public void clearCachedValues() {
        for (ParameterHolder p in classifierParameters.values()) {
            p.clearCachedValues()
        }
    }

    List<String> referencePaths(Class markerInterface, String refValue) {
        if (classifierParameters.size() > 0) {
            List<String> references = new ArrayList<String>()
            for (ParameterHolder parameterHolder: classifierParameters.values()) {
                if (parameterHolder instanceof MultiDimensionalParameterHolder) {
                    references.addAll parameterHolder.referencePaths(markerInterface, refValue)
                }
            }
            return references
        }
        return Collections.emptyList()
    }

    List<String> updateReferenceValues(Class markerInterface, String oldValue, String newValue) {
        if (classifierParameters.size() > 0) {
            List<String> referencePaths = new ArrayList<String>()
            for (ParameterHolder parameterHolder: classifierParameters.values()) {
                if (parameterHolder instanceof MultiDimensionalParameterHolder) {
                    referencePaths.addAll parameterHolder.updateReferenceValues(markerInterface, oldValue, newValue)
                }
            }
            return referencePaths
        }
        return Collections.emptyList()
    }
}
