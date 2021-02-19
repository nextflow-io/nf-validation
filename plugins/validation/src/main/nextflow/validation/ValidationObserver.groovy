package nextflow.validation

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.trace.TraceObserver

/**
 * Implements trace observer to intercept the flow creation
 * and validation the pipelines params
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ValidationObserver implements TraceObserver {

    public static final String SCHEMA_NAME = 'nextflow_schema.json'

    @Override
    void onFlowCreate(Session session) {
        final params = (Map) session.config.params
        final schema = session.baseDir.resolve(SCHEMA_NAME)
        if( schema.exists() ) {
            new SchemaValidator().validateParameters(params, schema.text)
        }
    }

}
