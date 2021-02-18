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

    @Override
    void onFlowCreate(Session session) {
       // TODO  
    }

}
