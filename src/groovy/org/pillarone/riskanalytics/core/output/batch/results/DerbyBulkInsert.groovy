package org.pillarone.riskanalytics.core.output.batch.results

import groovy.sql.Sql
import org.pillarone.riskanalytics.core.output.batch.AbstractBulkInsert

public class DerbyBulkInsert extends AbstractResultsBulkInsert {

    protected void writeResult(List values) {
        values << 0 //derby needs version column explicitly
        writer.writeLine(values.join(','))
    }

    protected void save() {

        long time = System.currentTimeMillis()
        Sql sql = new Sql(simulationRun.dataSource)

        String query = "CALL SYSCS_UTIL.SYSCS_IMPORT_DATA (NULL, 'SINGLE_VALUE_RESULT', 'SIMULATION_RUN_ID,PERIOD,ITERATION,PATH_ID,FIELD_ID,COLLECTOR_ID,VALUE,VERSION,VALUE_INDEX,DATE', null, '${tempFile.getAbsolutePath()}',null, null, null, 0) "
        sql.executeUpdate(query.replaceAll('\\\\', '/'))
        time = System.currentTimeMillis() - time
        LOG.info("results saved in ${time} ms");
        sql.close()
    }
}