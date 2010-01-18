package org.pillarone.riskanalytics.core.simulation.engine

import grails.test.GrailsUnitTestCase
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.core.simulation.SimulationState
import org.pillarone.riskanalytics.core.simulation.engine.IterationScope
import org.pillarone.riskanalytics.core.simulation.engine.PeriodScope
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRunner
import org.pillarone.riskanalytics.core.simulation.engine.SimulationScope
import org.pillarone.riskanalytics.core.simulation.engine.actions.Action
import org.pillarone.riskanalytics.core.simulation.engine.actions.IterationAction
import org.pillarone.riskanalytics.core.simulation.engine.actions.PeriodAction
import org.pillarone.riskanalytics.core.simulation.engine.actions.SimulationAction

class SimulationRunnerTests extends GrailsUnitTestCase {

    void testSimulationRun() {
        PeriodScope periodScope = new PeriodScope()
        IterationScope iterationScope = new IterationScope(periodScope: periodScope, numberOfPeriods: 2)
        SimulationScope simulationScope = new SimulationScope(iterationScope: iterationScope, numberOfIterations: 2)

        Action periodAction = new PeriodAction(periodScope: periodScope)
        Action iterationAction = new IterationAction(iterationScope: iterationScope, periodAction: periodAction)
        Action simulationAction = new SimulationAction(simulationScope: simulationScope, iterationAction: iterationAction)

        Action preSimulationAction = [perform: {LogFactory.getLog(Action).debug "performing preSimulationAction"}] as Action
        Action postSimulationAction = [perform: {LogFactory.getLog(Action).debug "performing postSimulationAction"}] as Action


        SimulationRunner runner = new SimulationRunner(currentScope: simulationScope)
        runner.preSimulationActions << preSimulationAction
        runner.simulationAction = simulationAction
        runner.postSimulationActions << postSimulationAction

        runner.start()

    }

    void testSimulationRunStopping() {
        PeriodScope periodScope = new PeriodScope()
        IterationScope iterationScope = new IterationScope(periodScope: periodScope, numberOfPeriods: 2)
        SimulationScope simulationScope = new SimulationScope(iterationScope: iterationScope, numberOfIterations: 10000)

        Action periodAction = new PeriodAction(periodScope: periodScope)
        Action iterationAction = new IterationAction(iterationScope: iterationScope, periodAction: periodAction)
        Action simulationAction = new SimulationAction(simulationScope: simulationScope, iterationAction: iterationAction)

        volatile boolean postSimulationActionCalled = false
        Action preSimulationAction = [perform: {LogFactory.getLog(Action).debug "performing preSimulationAction"}] as Action
        Action postSimulationAction = [perform: {postSimulationActionCalled = true}] as Action

        SimulationRunner runner = new SimulationRunner(currentScope: simulationScope)
        runner.preSimulationActions << preSimulationAction
        runner.simulationAction = simulationAction
        runner.postSimulationActions << postSimulationAction

        Thread.start {
            runner.start()
            assertTrue "stopped too late", simulationScope.currentIteration < simulationScope.numberOfIterations - 1
            assertTrue "postSimulationAction not performed", postSimulationActionCalled

        }
        Thread.sleep 2000
        runner.stop()
    }

    void testCreateRunner() {

        SimulationRunner runner = SimulationRunner.createRunner()
        assertNotNull "no runner created", runner
        SimulationScope simulationScope = runner.simulationAction.simulationScope
        assertNotNull "No simulationScope on action", simulationScope
        assertNotNull "no iterationscope defined", simulationScope.iterationScope
        assertNotNull "no periodcope defined", simulationScope.iterationScope.periodScope
        assertNotNull "no simulationaction defined", runner.simulationAction
        assertNotNull "no iterationaction defined", runner.simulationAction.iterationAction
        assertNotNull "no periodaction defined", runner.simulationAction.iterationAction.periodAction

    }

    void testErrorDuringSimulation() {
        PeriodScope periodScope = new PeriodScope()
        IterationScope iterationScope = new IterationScope(periodScope: periodScope, numberOfPeriods: 2)
        SimulationScope simulationScope = new SimulationScope(iterationScope: iterationScope, numberOfIterations: 10)

        PeriodAction periodAction = [perform: {throw new Exception()}] as PeriodAction
        Action iterationAction = new IterationAction(iterationScope: iterationScope, periodAction: periodAction)
        Action simulationAction = new SimulationAction(simulationScope: simulationScope, iterationAction: iterationAction)

        volatile boolean postSimulationActionCalled = false
        Action preSimulationAction = [perform: {LogFactory.getLog(Action).debug "performing preSimulationAction"}] as Action
        Action postSimulationAction = [perform: {postSimulationActionCalled = true}] as Action

        SimulationRunner runner = new SimulationRunner(currentScope: simulationScope)
        runner.preSimulationActions << preSimulationAction
        runner.simulationAction = simulationAction
        runner.postSimulationActions << postSimulationAction

        runner.start()

        assertSame "simulation state after error", SimulationState.ERROR, runner.simulationState
        assertNotNull "error object not set", runner.error
    }

    // TODO (Oct 21, 2009, msh): maybe dk has an idea why this test doesn't work
/*
    void testScopeConfiguration() {

        mockDomain ParameterizationDAO
        mockDomain ResultConfiguration
        mockDomain SimulationRun

        ParameterizationDAO params = new ParameterizationDAO(periodCount: 1, name:"params", modelClassName:"model", itemVersion:"1")
        assertNotNull params.save()
        ResultConfiguration resultConfig = new ResultConfiguration(name: "result", collectorInformation: [])
        assertNotNull resultConfig.save()
        println params.dump()
        println resultConfig.dump()
        SimulationRun simulationRun = new SimulationRun()
        simulationRun.name = "run"
        simulationRun.parameterization = params
        simulationRun.resultConfiguration = resultConfig
        simulationRun.model = CapitalEagleModel.name
        simulationRun.iterations = 10
        simulationRun.periodCount = 1
        println simulationRun.dump()
        assertNotNull simulationRun.save()

        FileOutput outputStrategy = new FileOutput()
        SimulationConfiguration configuration = new SimulationConfiguration(simulationRun: simulationRun, outputStrategy: outputStrategy)

        SimulationRunner runner = SimulationRunner.createRunner()
        assertNotNull "no runner created", runner

        runner.simulationConfiguration = configuration
        
        SimulationScope simulationScope = runner.simulationAction.simulationScope

        assertEquals "iterationCount", simulationRun.iterations, simulationScope.numberOfIterations
        assertEquals "model class", simulationRun.model, simulationScope.model.class.name
        assertSame "wrong simulationRun", simulationRun, simulationScope.simulationRun
        assertSame "wrong parameterization", simulationRun.parameterization, simulationScope.parameters
        assertSame "wrong result config", simulationRun.resultConfiguration, simulationScope.resultConfiguration
        assertSame "wrong output strategy", outputStrategy, simulationScope.outputStrategy

    }
*/


}