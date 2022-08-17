package nextflow.validation

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.trace.TraceObserverFactory
/**
 * Implements the validation observer factory
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ValidationFactory implements TraceObserverFactory {

    @Override
    Collection<TraceObserver> create(Session session) {
        final result = new ArrayList()
        result << new ValidationObserver()
        return result
    }
}
