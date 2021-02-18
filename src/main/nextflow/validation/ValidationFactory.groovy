package nextflow.validation


import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.trace.TraceObserverFactory
/**
 * Implements the validation observer factory
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ValidationFactory implements TraceObserverFactory {

    @Override
    Collection<TraceObserver> create(Session session) {
        return [new ValidationObserver()]
    }
}
