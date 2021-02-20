package nextflow.validation

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener

class SchemaValidator {

    static final List<String> NF_OPTIONS = [
            // Options for base `nextflow` command
            'bg',
            'c',
            'C',
            'config',
            'd',
            'D',
            'dockerize',
            'h',
            'log',
            'q',
            'quiet',
            'syslog',
            'v',
            'version',

            // Options for `nextflow run` command
            'ansi',
            'ansi-log',
            'bg',
            'bucket-dir',
            'c',
            'cache',
            'config',
            'dsl2',
            'dump-channels',
            'dump-hashes',
            'E',
            'entry',
            'latest',
            'lib',
            'main-script',
            'N',
            'name',
            'offline',
            'params-file',
            'pi',
            'plugins',
            'poll-interval',
            'pool-size',
            'profile',
            'ps',
            'qs',
            'queue-size',
            'r',
            'resume',
            'revision',
            'stdin',
            'stub',
            'stub-run',
            'test',
            'w',
            'with-charliecloud',
            'with-conda',
            'with-dag',
            'with-docker',
            'with-mpi',
            'with-notification',
            'with-podman',
            'with-report',
            'with-singularity',
            'with-timeline',
            'with-tower',
            'with-trace',
            'with-weblog',
            'without-docker',
            'without-podman',
            'work-dir'
    ]

    private List<String> errors = []
    private List<String> warnings = []

    boolean hasErrors() { errors.size()>0 }
    List<String> getErrors() { errors }

    boolean hasWarnings() { warnings.size()>0 }
    List<String> getWarnings() { warnings }

    /*
    * Function to loop over all parameters defined in schema and check
    * whether the given paremeters adhere to the specificiations
    */
    /* groovylint-disable-next-line UnusedPrivateMethodParameter */
    void validateParameters(Map params, String json) {

        // Clean the parameters
        def cleanedParams = cleanParameters(params)
        // Convert to JSONObject
        def paramsJSON = new JSONObject(new JsonBuilder(cleanedParams).toString())

        //=====================================================================//
        // Check for nextflow core params and unexpected params
        def Map schemaParams = (Map) new JsonSlurper().parseText(json).get('definitions')
        def specifiedParamKeys = params.keySet()

        // Collect expected parameters from the schema
        def expectedParams = []
        for (group in schemaParams) {
            for (p in group.value['properties']) {
                expectedParams.push(p.key)
            }
        }

        for (specifiedParam in specifiedParamKeys) {
            // nextflow params
            if (NF_OPTIONS.contains(specifiedParam)) {
                errors << "You used a core Nextflow option with two hyphens: '--${specifiedParam}'. Please resubmit with '-${specifiedParam}'".toString()
            }
            // unexpected params
            def params_ignore = params.schema_ignore_params ? params.schema_ignore_params.split(',') + 'schema_ignore_params' : []
            if (!expectedParams.contains(specifiedParam) && !params_ignore.contains(specifiedParam)) {
                warnings << "* --${specifiedParam}: ${paramsJSON[specifiedParam]}".toString()
            }
        }

        //=====================================================================//
        // Validate parameters against the schema
        final rawSchema = new JSONObject(new JSONTokener(json))
        final schema = SchemaLoader.load(rawSchema)

        // Validate
        try {
            schema.validate(paramsJSON)
        }
        catch (ValidationException e) {
            JSONObject exceptionJSON = e.toJSON()
            collectErrors(exceptionJSON, paramsJSON)
        }
    }

    // Loop over nested exceptions and print the causingException
    private void collectErrors(JSONObject exJSON, JSONObject paramsJSON) {
        def causingExceptions = exJSON['causingExceptions']
        if (causingExceptions.length() == 0) {
            def m = exJSON['message'] =~ /required key \[([^\]]+)\] not found/
            // Missing required param
            if(m.matches()){
                errors << "* Missing required parameter: --${m[0][1]}".toString()
            }
            // Other base-level error
            else if(exJSON['pointerToViolation'] == '#'){
                errors << "* ${exJSON['message']}".toString()
            }
            // Error with specific param
            else {
                def param = exJSON['pointerToViolation'] - ~/^#\//
                def param_val = paramsJSON[param].toString()
                errors << "* --${param}: ${exJSON['message']} (${param_val})".toString()
            }
        }
        for (ex in causingExceptions) {
            collectErrors(ex, paramsJSON)
        }
    }

    private static Map cleanParameters(params) {
        def new_params = params.getClass().newInstance(params)
        for (p in params) {
            // remove anything evaluating to false
            if (!p['value']) {
                new_params.remove(p.key)
            }
            // Cast MemoryUnit to String
            if (p['value'].getClass() == nextflow.util.MemoryUnit) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Cast Duration to String
            if (p['value'].getClass() == nextflow.util.Duration) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Cast LinkedHashMap to String
            if (p['value'].getClass() == LinkedHashMap) {
                new_params.replace(p.key, p['value'].toString())
            }
        }
        return new_params
    }

}
