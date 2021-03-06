package org.pillarone.riskanalytics.core.parameterization

import org.pillarone.riskanalytics.core.model.IModelVisitor
import org.pillarone.riskanalytics.core.model.ModelPath

interface IParameterObject {
    IParameterObjectClassifier getType()

    Map getParameters()

    void accept(IModelVisitor visitor, ModelPath path)

}