package org.pillarone.riskanalytics.core.simulation.engine.actions

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.output.CollectorFactory
import org.pillarone.riskanalytics.core.output.PacketCollector
import org.pillarone.riskanalytics.core.simulation.engine.SimulationScope
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration

public class WireModelAction implements Action {

    private static Log LOG = LogFactory.getLog(WireModelAction)

    SimulationScope simulationScope

    public void perform() {
        LOG.debug "Wiring model"
        synchronized (this.getClass()) {
            Model model = simulationScope.model
            ResultConfiguration resultConfig = simulationScope.resultConfiguration

            model.wire()
            LOG.debug "Model wired"


            CollectorFactory collectorFactory = simulationScope.collectorFactory
            collectorFactory.structureInformation = simulationScope.structureInformation

            //PMO-654: make sure that the collectors are always wired in the same order to enable robust model reference result comparison
            List collectors = resultConfig.getResolvedCollectors(model, collectorFactory).sort { it.path }
            collectors.each {PacketCollector it ->
                it.attachToModel(model, simulationScope.structureInformation)
            }

            LOG.debug "Collectors attached"

            LOG.debug "Optimizing wiring"
            model.optimizeComposedComponentWiring()
        }
    }


}
