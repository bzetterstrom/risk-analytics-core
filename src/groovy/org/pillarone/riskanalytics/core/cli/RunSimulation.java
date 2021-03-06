package org.pillarone.riskanalytics.core.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.pillarone.riskanalytics.core.initialization.GrailsEnvironment;
import org.pillarone.riskanalytics.core.initialization.IExternalDatabaseSupport;
import org.pillarone.riskanalytics.core.initialization.StandaloneConfigLoader;
import org.pillarone.riskanalytics.core.simulation.engine.RunSimulationService;
import org.pillarone.riskanalytics.core.simulation.engine.SimulationConfiguration;
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRunner;
import org.pillarone.riskanalytics.core.simulation.item.Simulation;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.support.AbstractApplicationContext;

import java.io.PrintWriter;
import java.io.StringWriter;


public class RunSimulation {

    private static Log LOG = LogFactory.getLog(RunSimulation.class);

    public static void main(String[] args) throws Exception {
        IExternalDatabaseSupport databaseSupport = null;
        try {
            String env = System.getProperty("grails.env") != null ? System.getProperty("grails.env") : "environment";
            databaseSupport = StandaloneConfigLoader.getExternalDatabaseSupport(env);
            if (databaseSupport != null) {
                databaseSupport.startDatabase();
            }


            System.setProperty("skipImport", "true");
            GrailsEnvironment.setUp();
            ArgumentParser parser = new ArgumentParser();
            CommandLine commandLine = parser.parseArguments(args);
            if (commandLine == null) {
                StringWriter writer = new StringWriter();
                parser.printHelp(new PrintWriter(writer));
                LOG.error(writer.toString());
                shutdown(databaseSupport);
                return;
            }

            SimulationRunner runner = SimulationRunner.createRunner();
            SimulationConfiguration configuration = createConfiguration(parser, commandLine);

            ImportStructureInTransaction.importStructure(configuration);

            RunSimulationService.getService().runSimulation(runner, configuration);
            SimulationLogger logger = new SimulationLogger(runner, LOG);
            synchronized (logger) {
                logger.wait();
            }
            LOG.info("Simulation completed with simulation run id: " + runner.getCurrentScope().getSimulation().id);
        } catch (Throwable t) {
            LOG.error("Simulation run failed", t);
        } finally {
            shutdown(databaseSupport);
        }
    }


    private static SimulationConfiguration createConfiguration(ArgumentParser parser, CommandLine commandLine) throws Exception {
        SimulationConfiguration configuration = new SimulationConfiguration();
        configuration.setOutputStrategy(parser.getOutputStrategy(commandLine));
        Simulation simulation = parser.createSimulation(commandLine);
        simulation.getParameterization().load(true);
        simulation.getTemplate().load(true);
        configuration.setSimulation(simulation);
        return configuration;
    }

    private static void shutdown(IExternalDatabaseSupport databaseSupport) {
        AbstractApplicationContext applicationContext = (AbstractApplicationContext) ApplicationHolder.getApplication().getMainContext();
        applicationContext.close();
        try {
            StdSchedulerFactory.getDefaultScheduler().shutdown();
        } catch (SchedulerException e) {
            LOG.warn("Error shutting down quartz scheduler", e);
        }
        if (databaseSupport != null) {
            databaseSupport.stopDatabase();
        }
    }
}

