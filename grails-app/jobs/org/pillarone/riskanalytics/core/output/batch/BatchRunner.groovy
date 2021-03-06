package org.pillarone.riskanalytics.core.output.batch

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.pillarone.riskanalytics.core.batch.BatchRunService
import org.quartz.Job
import org.quartz.JobExecutionContext

public class BatchRunner implements Job {

    static final Logger LOG = Logger.getLogger(BatchRunner)

    public static BatchRunService getService() {
        return ApplicationHolder.getApplication().getMainContext().getBean('batchRunService')
    }

    public void execute(JobExecutionContext jobExecutionContext) {
        synchronized (this) {
            getService().runBatches()
        }
    }

}


