package org.pillarone.riskanalytics.core.batch

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.pillarone.riskanalytics.core.BatchRun
import org.pillarone.riskanalytics.core.BatchRunSimulationRun
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.cli.ImportStructureInTransaction
import org.pillarone.riskanalytics.core.output.ICollectorOutputStrategy
import org.pillarone.riskanalytics.core.output.OutputStrategy
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.output.batch.OutputStrategyFactory
import org.pillarone.riskanalytics.core.simulation.SimulationState
import org.pillarone.riskanalytics.core.simulation.engine.RunSimulationService
import org.pillarone.riskanalytics.core.simulation.engine.SimulationConfiguration
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRunner
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.joda.time.DateTime

class BatchRunService {

    boolean transactional = false
    def batchRunInfoService
    BatchRunSimulationRun addedBatchRunSimulationRun
    RunnerRegistry runnerRegistry
    Log LOG = LogFactory.getLog(BatchRunService)

    public static BatchRunService getService() {
        return ApplicationHolder.getApplication().getMainContext().getBean('batchRunService')
    }

    public void runBatches() {
        getActiveBatchRuns()?.each {BatchRun batchRun ->
            runBatch(batchRun)
        }
    }

    public void runBatch(BatchRun batchRun) {
        getSimulationRuns(batchRun)?.each {BatchRunSimulationRun batchRunSimulationRun ->
            runSimulation(batchRunSimulationRun)
        }
        BatchRun.executeUpdate("update org.pillarone.riskanalytics.core.BatchRun as b set b.executed=? where b.id=? ", [true, batchRun.id])
    }

    public synchronized void runSimulation(BatchRunSimulationRun batchRunSimulationRun) {
        if (batchRunSimulationRun.simulationRun.endTime == null && !batchRunInfoService.runningSimulations.contains(batchRunSimulationRun.simulationRun.id)) {
            batchRunInfoService.runningSimulations << batchRunSimulationRun.simulationRun.id
            batchRunInfoService.addActiveSimulationRun batchRunSimulationRun.simulationRun
            ICollectorOutputStrategy strategy = OutputStrategyFactory.getInstance(batchRunSimulationRun.strategy)

            SimulationRunner runner = SimulationRunner.createRunner()
            Simulation simulation = createSimulation(batchRunSimulationRun.simulationRun.name)
            SimulationConfiguration configuration = new SimulationConfiguration(simulation: simulation, outputStrategy: strategy)

            runner.batchRunInfoService = batchRunInfoService

            ImportStructureInTransaction.importStructure(configuration);
            getRunnerRegistry().put(runner, configuration)
        } else {
            LOG.info "simulation ${batchRunSimulationRun.simulationRun.name} is already executed at ${batchRunSimulationRun.simulationRun.endTime}"
        }
    }


    void addSimulationRun(BatchRun batchRun, Simulation simulation, OutputStrategy strategy) {
        BatchRun.withTransaction {
            simulation.save()
            batchRun = BatchRun.findByName(batchRun.name)
            int priority = BatchRunSimulationRun.countByBatchRun(batchRun)
            addedBatchRunSimulationRun = new BatchRunSimulationRun(batchRun: batchRun, simulationRun: simulation.simulationRun, priority: priority, strategy: strategy, simulationState: SimulationState.NOT_RUNNING)
            addedBatchRunSimulationRun.save()
            if (batchRun.executed) {
                batchRun.executed = false
                batchRun.save()
            }
        }
    }

    SimulationRun getSimulationRunAt(BatchRun batchRun, int index) {
        BatchRun.withTransaction {
            List<SimulationRun> runs = getSimulationRuns(batchRun)*.simulationRun
            return runs?.get(index)
        }
    }

    List<BatchRunSimulationRun> getSimulationRuns(BatchRun batchRun) {
        return BatchRunSimulationRun.findAllByBatchRun(batchRun, [sort: "priority", order: "asc"])
    }

    public BatchRunSimulationRun getSimulationRun(BatchRun batchRun, SimulationRun simulationRun) {
        BatchRunSimulationRun item = null
        BatchRunSimulationRun.withTransaction {
            item = BatchRunSimulationRun.findByBatchRunAndSimulationRun(batchRun, simulationRun)
            item.simulationRun = SimulationRun.findByName(simulationRun.name)
            item.simulationRun.parameterization = ParameterizationDAO.find(simulationRun.parameterization.name, simulationRun.model, simulationRun.parameterization.itemVersion.toString())
        }
        return item
    }


    void deleteSimulationRun(BatchRun batchRun, SimulationRun simulationRun) {
        BatchRun.withTransaction {
            BatchRunSimulationRun batchRunSimulationRun = BatchRunSimulationRun.findByBatchRunAndSimulationRun(batchRun, simulationRun)
            batchRunSimulationRun.delete()
        }
    }

    synchronized void changePriority(BatchRun batchRun, SimulationRun simulationRun, int step) {
        BatchRun.withTransaction {
            BatchRunSimulationRun batchRunSimulationRun = BatchRunSimulationRun.findByBatchRunAndSimulationRun(batchRun, simulationRun)
            int newPriority = batchRunSimulationRun.priority + step
            BatchRunSimulationRun bRSRun = BatchRunSimulationRun.findByPriority(newPriority)
            if (bRSRun != null) {
                bRSRun.priority = batchRunSimulationRun.priority
                bRSRun.save()
                batchRunSimulationRun.priority = newPriority
                batchRunSimulationRun.save()
            }
        }
    }

    boolean deleteBatchRun(BatchRun batchRun) {
        BatchRun.withTransaction {
            try {
                BatchRunSimulationRun.executeUpdate("delete from org.pillarone.riskanalytics.core.BatchRunSimulationRun as b where b.batchRun=?", [batchRun])
                BatchRun.executeUpdate("delete from org.pillarone.riskanalytics.core.BatchRun as b where b.id=?", [batchRun.id])
                return true
            } catch (Exception ex) {
                LOG.error "Exception occured during delete of BatchRun : ${batchRun.name}"
            }
            return false
        }
    }


    public List<BatchRun> getActiveBatchRuns() {
        return BatchRun.executeQuery("from org.pillarone.riskanalytics.core.BatchRun as b where b.executed = :executed and b.executionTime <= :cDate order by b.executionTime asc ", ["cDate": new DateTime(), "executed": false])
    }

    public List<BatchRun> getAllBatchRuns() {
        return BatchRun.executeQuery("from org.pillarone.riskanalytics.core.BatchRun as b order by b.executionTime asc")
    }

    private Simulation createSimulation(String simulationName) {
        Simulation simulation = new Simulation(simulationName)
        simulation.load()
        simulation.getParameterization().load();
        simulation.getTemplate().load();
        return simulation
    }

    RunnerRegistry getRunnerRegistry() {
        if (!runnerRegistry) runnerRegistry = new RunnerRegistry(batchRunInfoService)
        return runnerRegistry
    }

}

class RunnerRegistry implements ActionListener {
    def batchRunInfoService
    Queue queue = new LinkedList()
    private javax.swing.Timer timer
    SimulationRunner simulationRunner


    Log LOG = LogFactory.getLog(RunnerRegistry)

    public RunnerRegistry(def batchRunInfoService) {
        this.batchRunInfoService = batchRunInfoService
        queue = new LinkedList()
        timer = new javax.swing.Timer(5000, this)
        timer.setRepeats(true)
    }

    void put(SimulationRunner runner, SimulationConfiguration configuration) {
        queue.offer(["runner": runner, "configuration": configuration])
        if (!timer.isRunning()) timer.start()
    }

    void actionPerformed(ActionEvent e) {
        if (!simulationRunner) {
            simulationRunner = pollAndRun()
        } else if (simulationRunner.simulationState == SimulationState.FINISHED || simulationRunner.simulationState == SimulationState.ERROR) {
            simulationRunner = pollAndRun()
            if (!simulationRunner) {
                timer.stop()
                LOG.info "no simulation to execute "
            }
        }
    }

    private SimulationRunner pollAndRun() {
        SimulationRunner simulationRunner = null
        def item = queue.poll()
        if (item) {
            simulationRunner = item["runner"]
            SimulationConfiguration configuration = item["configuration"]
            RunSimulationService.getService().runSimulation(simulationRunner, configuration)
            LOG.info "executing a simulation ${configuration.simulation.name} at ${new DateTime()}"
        }
        return simulationRunner
    }

    protected void notifySimulationStart(Simulation simulation, SimulationState simulationState) {
        batchRunInfoService?.batchSimulationStart(simulation)
    }


}
