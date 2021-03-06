package org.pillarone.riskanalytics.core.parameterization

import com.google.common.collect.MapMaker
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class ConstraintsFactory {

    private static Log LOG = LogFactory.getLog(ConstraintsFactory)

    private static Map<String, IMultiDimensionalConstraints> constraints = new MapMaker().makeMap()

    static void registerConstraint(IMultiDimensionalConstraints constraint) {
        String identifier = constraint.name
        IMultiDimensionalConstraints existingConstraint = constraints.get(identifier)
        if (existingConstraint == null) {
            constraints.put(identifier, constraint)
        } else {
            if (existingConstraint.getClass().name == constraint.getClass().name) {
                LOG.warn "Constraint $identifier already exists - ignoring"
            } else {
                throw new IllegalStateException("Constraint $identifier already associated with ${existingConstraint.getClass().name}")
            }
        }
    }

    static IMultiDimensionalConstraints getConstraints(String name) {
        return loadConstraint(name)
    }

    protected static IMultiDimensionalConstraints loadConstraint(String name) {
        IMultiDimensionalConstraints constraint = constraints.get(name)
        if (constraint != null) {
            //workaround for migration
            Class clazz = Thread.currentThread().contextClassLoader.loadClass(constraint.class.name)
            if (clazz != constraint.class) {
                return clazz.newInstance()
            }
        }
        return constraint
    }
}