package nextflow.validation

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.trace.TraceObserver

/**
 * Implements trace observer to intercept the flow creation
 * and validation the pipelines params
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class ValidationObserver implements TraceObserver {

    public static final String SCHEMA_NAME = 'nextflow_schema.json'

    private SchemaValidator validator

    @Override
    void onFlowCreate(Session session) {
        final params = (Map) session.config.params
        final schema = session.baseDir.resolve(SCHEMA_NAME)
        if( !schema.exists() ) {
            return
        }
        log.debug "Applying validation schema at path: $schema"
        validator = new SchemaValidator()
        if (!params.help) {
            validator.validateParameters(params, session.baseDir, schema.text)
        }
        // check for errors
        if( validator.errors ) {
            def msg = "The following invalid input values have been detected:\n" + validator.errors.join('\n').trim()
            throw new SchemaValidationException(msg, validator.errors)
        }
        // check for warnings
        if( validator.warnings ) {
            def msg = "The following invalid input values have been detected:\n" + validator.warnings.join('\n').trim()
            log.warn(msg)
        }

    }

}
