package nextflow.validation

import groovy.util.logging.Slf4j
import groovy.transform.PackageScope


/**
 * This class allows model an specific configuration, extracting values from a map and converting
 *
 *
 * when the plugin reverse a String it will append '>>' at the beginning instead the default 'Mr.'
 *
 * We anotate this class as @PackageScope to restrict the access of their methods only to class in the
 * same package
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
@PackageScope
class ValidationConfig {

    final private Boolean lenientMode
    final private Boolean monochromeLogs
    final private Boolean failUnrecognisedParams
    final private Boolean showHiddenParams

    final private List<String> ignoreParams

    ValidationConfig(Map map){
        def config = map ?: Collections.emptyMap()
        lenientMode             = config.lenientMode            ?: false
        monochromeLogs          = config.monochromeLogs         ?: false
        failUnrecognisedParams  = config.failUnrecognisedParams ?: false
        showHiddenParams        = config.showHiddenParams       ?: false

        if(config.ignoreParams && !(config.ignoreParams instanceof List<String>)) {
            throw new SchemaValidationException("Config value 'validation.ignoreParams' should be a list of String values")
        }
        ignoreParams = config.ignoreParams ?: []
        if(config.defaultIgnoreParams && !(config.defaultIgnoreParams instanceof List<String>)) {
            throw new SchemaValidationException("Config value 'validation.defaultIgnoreParams' should be a list of String values")
        }
        ignoreParams += config.defaultIgnoreParams ?: []
    }

    String getPrefix() { prefix }
}