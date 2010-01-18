package org.pillarone.riskanalytics.core.simulation.engine

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter
import org.pillarone.riskanalytics.core.simulation.SimulationState
import org.pillarone.riskanalytics.core.simulation.engine.actions.*

/**
 * This is the main entity to run a simulation. To do this, create a runner object (SimulationRunner.createRunner()).
 * A simulation run is performed in 3 different phases. These 3 phases are executed by performing
 *
 * - the preSimulationActions, where the model is initialized and wired, parameters are loaded and prepared,
 * the collector instances get created and attached to the model. The different task are implemented as Actions.
 *
 * - simulationAction, where the iterative simulation is done
 *
 * - the postSimulationActions, where any operations are done to finish a simulation
 * e.g. uploading the batch file for persistence, doing postSimulationCalculation.
 * The different task are implemented as Actions
 *
 *
 * A SimulationRunner instance has to be configured with a SimulationConfiguration before starting a simulation.
 * Each simulation has to use a new SimulationRunner instance.
 */

public class SimulationRunner {

    private static Log LOG = LogFactory.getLog(SimulationRunner)
    private long start

    List preSimulationActions = []
    SimulationAction simulationAction
    List postSimulationActions = []

    SimulationScope currentScope

    SimulationError error

    /**
     * Starting a simulation run by performing the
     *
     * - preSimulationActions,
     * - the simulationAction and
     * - the postSimulationActions
     *
     * Any exception occuring during the simulation is caught and the error object will be initialized.
     */
    public void start() {
        simulationState = SimulationState.INITIALIZING
        LOG.debug "start simulation"
        start = System.currentTimeMillis()
        currentScope?.simulationRun?.startTime = new Date(start)
        try {
            LOG.debug "perform preSimulationActions"
            for (Action action in preSimulationActions) {
                action.perform()
            }
            LOG.debug "perform simulationAction"
            simulationState = SimulationState.RUNNING
            simulationAction.perform()

            LOG.debug "perform postSimulationActions"
            for (Action action in postSimulationActions) {
                action.perform()
            }
        } catch (Throwable t) {
            simulationState = SimulationState.ERROR
            error = new SimulationError(
                    simulationRunID: currentScope.simulationRun?.id,
                    iteration: currentScope.iterationScope.currentIteration,
                    period: currentScope.iterationScope.periodScope.currentPeriod,
                    error: t
            )
            LOG.error this, t
            LOG.debug error.dump()
            return
        }

        LOG.debug "end simulation"
        simulationState = SimulationState.FINISHED
        long end = System.currentTimeMillis()
        currentScope?.simulationRun?.endTime = new Date(end)
        currentScope?.simulationRun?.save(flush: true)
        LogFactory.getLog(SimulationRunner).info "simulation took ${end - start} ms"

    }

    /**
     * The current simulation will be stopped at the next iteration. The simulationState will indicate, that
     * the simulation has been stopped.
     */
    public void stop() {
        simulationAction.stop()
        simulationState = SimulationState.STOPPED
    }

    Date getEstimatedSimulationEnd() {
        int progress = currentScope.getProgress()
        if (progress > 0 && simulationState == SimulationState.RUNNING) {
            long now = System.currentTimeMillis()
            long onePercentTime = (now - start) / progress
            long estimatedEnd = now + (onePercentTime * (100 - progress))
            return new Date(estimatedEnd)
        } else if (simulationState == SimulationState.POST_SIMULATION_CALCULATIONS) {
            CalculatorAction action = postSimulationActions.find { it instanceof CalculatorAction }
            return action?.calculator?.estimatedEnd
        }
        return null
    }

    int getProgress() {
        if (simulationState == SimulationState.POST_SIMULATION_CALCULATIONS) {
            CalculatorAction action = postSimulationActions.find { it instanceof CalculatorAction }
            return action?.calculator?.progress
        } else {
            return currentScope.progress
        }
    }

    /**
     * Configure the runner with the passed configuration.
     * All information about the simulation will be gathered from the configuration and the actions and scopes get the requiered parameter.
     */
    public void setSimulationConfiguration(SimulationConfiguration configuration) {
        SimulationRun run = SimulationRun.get(configuration.simulationRun.id)
        currentScope.simulationRun = run
        currentScope.model = this.class.classLoader.loadClass(run.model).newInstance()
        currentScope.outputStrategy = configuration.outputStrategy
        currentScope.iterationScope.numberOfPeriods = run.periodCount

        simulationAction.iterationAction.periodAction.model = currentScope.model
    }

    /**
     * Create a new instance for running a simulation. All Actions and Scopes get created.
     * The runner instance has to be configured with a SimulationConfiguration before being able to run.
     */
    public static SimulationRunner createRunner() {

        PeriodScope periodScope = new PeriodScope()
        IterationScope iterationScope = new IterationScope(periodScope: periodScope)
        SimulationScope simulationScope = new SimulationScope(iterationScope: iterationScope)


        Action initModel = new InitModelAction(simulationScope: simulationScope)
        Action randomSeed = new RandomSeedAction(simulationScope: simulationScope)
        Action initParams = new PrepareParameterizationAction(simulationScope: simulationScope, periodScope: periodScope)
        Action periodCounter = new CreatePeriodCounterAction(simulationScope: simulationScope)
        Action prepareStructure = new PrepareStructureInformationAction(simulationScope: simulationScope)
        Action wireModel = new WireModelAction(simulationScope: simulationScope)
        Action injectScopes = new InjectScopesAction(
                simulationScope: simulationScope,
                iterationScope: iterationScope,
                periodScope: periodScope
        )

        PeriodAction periodAction = new PeriodAction(periodScope: periodScope, model: simulationScope.model)
        IterationAction iterationAction = new IterationAction(periodAction: periodAction, iterationScope: iterationScope)
        SimulationAction simulationAction = new SimulationAction(iterationAction: iterationAction, simulationScope: simulationScope)

        Action calculatorAction = new CalculatorAction(simulationScope: simulationScope)
        Action finishOutputAction = new FinishOutputAction(simulationScope: simulationScope)

        SimulationRunner runner = new SimulationRunner()

        //The order of the pre & post simulation actions is important.
        // PrepareParameterizationAction must be before CreatePeriodCounterAction
        // PrepareStructureInformationAction must be before WireModelAction
        runner.preSimulationActions << initModel
        runner.preSimulationActions << randomSeed
        runner.preSimulationActions << initParams
        runner.preSimulationActions << periodCounter
        runner.preSimulationActions << prepareStructure
        runner.preSimulationActions << wireModel
        runner.preSimulationActions << injectScopes

        runner.simulationAction = simulationAction

        runner.postSimulationActions << finishOutputAction
        runner.postSimulationActions << calculatorAction

        runner.currentScope = simulationScope

        return runner
    }

    public SimulationState getSimulationState() {
        return currentScope.simulationState
    }

    protected void setSimulationState(SimulationState newState) {
        currentScope.simulationState = newState
    }
}