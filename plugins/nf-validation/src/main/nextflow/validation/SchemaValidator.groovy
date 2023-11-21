package nextflow.validation

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.json.JsonGenerator
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowWriteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern
import nextflow.extension.CH
import nextflow.Channel
import nextflow.Global
import nextflow.Nextflow
import nextflow.plugin.extension.Factory
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nextflow.script.WorkflowMetadata
import nextflow.Session
import nextflow.util.Duration
import nextflow.util.MemoryUnit
import org.everit.json.schema.loader.SchemaLoader
import org.everit.json.schema.PrimitiveValidationStrategy
import org.everit.json.schema.ValidationException
import org.everit.json.schema.Validator
import org.everit.json.schema.Schema
import org.json.JSONException
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.yaml.snakeyaml.Yaml

import static SamplesheetConverter.getHeader
import static SamplesheetConverter.getFileType

@Slf4j
@CompileStatic
class SchemaValidator extends PluginExtensionPoint {

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

    @Override
    protected void init(Session session) {
        // only called in operators
    }

    Session getSession(){
        Global.getSession() as Session
    }

    boolean hasErrors() { errors.size()>0 }
    List<String> getErrors() { errors }

    boolean hasWarnings() { warnings.size()>0 }
    List<String> getWarnings() { warnings }

    //
    // Resolve Schema path relative to main workflow directory
    //
    static String getSchemaPath(String baseDir, String schemaFilename='nextflow_schema.json') {
        if (Path.of(schemaFilename).exists()) {
            return schemaFilename
        } else {
            return "${baseDir}/${schemaFilename}"
        }
    }

    //
    // Find a value in a nested map
    //
    def findDeep(Map m, String key) {
        if (m.containsKey(key)) return m[key]
        m.findResult { k, v -> v instanceof Map ? findDeep(v, key) : null }
    }

    @Factory
    public DataflowWriteChannel fromSamplesheet(
        Map options = null,
        String samplesheetParam
    ) {
        def String baseDir = session.baseDir
        def Map params = session.params

        // Set defaults for optional inputs
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'
        def Boolean skipDuplicateCheck = options?.containsKey('skip_duplicate_check') ? options.skip_duplicate_check as Boolean : params.validationSkipDuplicateCheck ? params.validationSkipDuplicateCheck as Boolean : false

        def slurper = new JsonSlurper()
        def Map parsed = (Map) slurper.parse( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        def Map samplesheetValue = (Map) findDeep(parsed, samplesheetParam)
        def Path samplesheetFile = params[samplesheetParam] as Path
        if (samplesheetFile == null) {
            log.error "Parameter '--$samplesheetParam' was not provided. Unable to create a channel from it."
            throw new SchemaValidationException("", [])
        }
        def Path schemaFile = null
        if (samplesheetValue == null) {
            log.error "Parameter '--$samplesheetParam' was not found in the schema ($schemaFilename). Unable to create a channel from it."
            throw new SchemaValidationException("", [])
        }
        else if (samplesheetValue.containsKey('schema')) {
            schemaFile = Path.of(getSchemaPath(baseDir, samplesheetValue['schema'].toString()))
        } else {
            log.error "Parameter '--$samplesheetParam' does not contain a schema in the parameter schema ($schemaFilename). Unable to create a channel from it."
            throw new SchemaValidationException("", [])
        }

        log.debug "Starting validation: '$samplesheetFile' with '$schemaFile'"

        // Validate samplesheet
        def String fileType = SamplesheetConverter.getFileType(samplesheetFile)
        def String delimiter = fileType == "csv" ? "," : fileType == "tsv" ? "\t" : null
        def List<Map<String,String>> fileContent
        def Boolean s3PathCheck = params.validationS3PathCheck ? params.validationS3PathCheck : false
        def Map types = variableTypes(schemaFile.toString(), baseDir)
        def Boolean containsHeader = !(types.keySet().size() == 1 && types.keySet()[0] == "")

        if(!containsHeader){
            types = ["empty": types[""]]
        }
        if(fileType == "yaml"){
            fileContent = new Yaml().load((samplesheetFile.text)).collect {
                if(containsHeader) {
                    return it as Map
                }
                return ["empty": it] as Map
            }
        }
        else {
            fileContent = samplesheetFile.splitCsv(header:containsHeader ?: ["empty"], strip:true, sep:delimiter, quote:'\"')
        }
        def List<Map<String,String>> fileContentCasted = castToType(fileContent, types)
        if (validateFile(false, samplesheetFile.toString(), fileContentCasted, schemaFile.toString(), baseDir, s3PathCheck)) {
            log.debug "Validation passed: '$samplesheetFile' with '$schemaFile'"
        }

        // Convert to channel
        final channel = CH.create()
        List arrayChannel = SamplesheetConverter.convertToList(samplesheetFile, schemaFile, skipDuplicateCheck)
        session.addIgniter {
            arrayChannel.each { 
                channel.bind(it) 
            }
            channel.bind(Channel.STOP)
        }
        return channel
    }


    //
    // Initialise expected params if not present
    //
    Map initialiseExpectedParams(Map params) {
        if( !params.containsKey("validationFailUnrecognisedParams") ) {
            params.validationFailUnrecognisedParams = false
        }
        if( !params.containsKey("validationLenientMode") ) {
            params.validationLenientMode = false
        }
        if( !params.containsKey("help") ) {
            params.help = false
        }
        if( !params.containsKey("validationShowHiddenParams") ) {
            params.validationShowHiddenParams = false
        }
        if( !params.containsKey("validationSchemaIgnoreParams") ) {
            params.validationSchemaIgnoreParams = false
        }
        if( !params.containsKey("validationSkipDuplicateCheck") ) {
            params.validationSkipDuplicateCheck = false
        }
        if( !params.containsKey("validationS3PathCheck") ) {
            params.validationS3PathCheck = false
        }
        if( !params.containsKey("monochromeLogs") ) {
            params.monochromeLogs = false
        }
        if( !params.containsKey("monochrome_logs") ) {
            params.monochrome_logs = false
        }

        return params
    }


    //
    // Add expected params
    //
    List addExpectedParams() {
        def List expectedParams = [
            "validationFailUnrecognisedParams",
            "validationLenientMode",
            "help",
            "validationShowHiddenParams",
            "validationSchemaIgnoreParams",
            "validationSkipDuplicateCheck",
            "validationS3PathCheck",
            "monochromeLogs",
            "monochrome_logs"
        ]

        return expectedParams
    }

    /*
    * Function to loop over all parameters defined in schema and check
    * whether the given parameters adhere to the specifications
    */
    @Function
    void validateParameters(
        Map options = null
    ) {

        def Map params = initialiseExpectedParams(session.params)
        def String baseDir = session.baseDir
        def Boolean s3PathCheck = params.validationS3PathCheck ? params.validationS3PathCheck : false
        def Boolean useMonochromeLogs = options?.containsKey('monochrome_logs') ? options.monochrome_logs as Boolean :
            params.monochrome_logs ? params.monochrome_logs as Boolean : 
            params.monochromeLogs  ? params.monochromeLogs as Boolean :
            false
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'
        log.debug "Starting parameters validation"
        
        // Clean the parameters
        def cleanedParams = cleanParameters(params)
        // Convert to JSONObject
        def paramsJSON = new JSONObject(new JsonBuilder(cleanedParams).toString())

        //=====================================================================//
        // Check for nextflow core params and unexpected params
        def slurper = new JsonSlurper()
        def Map parsed = (Map) slurper.parse( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        def Map schemaParams = (Map) parsed.get('definitions')
        def specifiedParamKeys = params.keySet()

        // Collect expected parameters from the schema
        def enumsTuple = collectEnums(schemaParams)
        def List expectedParams = (List) enumsTuple[0] + addExpectedParams()
        def Map enums = (Map) enumsTuple[1]
        // Collect expected parameters from the schema when parameters are specified outside of "definitions"
        if (parsed.containsKey('properties')) {
            def enumsTupleTopLevel = collectEnums(['top_level': ['properties': parsed.get('properties')]])
            expectedParams += (List) enumsTupleTopLevel[0]
            enums += (Map) enumsTupleTopLevel[1]
        }

        //=====================================================================//
        // Check if files or directories exist
        def List<String> pathsToCheck = (List) collectExists(schemaParams)
        pathsToCheck.each {
            if (params[it]) {
                pathExists(params[it].toString(), it.toString(), s3PathCheck)
            }
        }

        def Boolean lenientMode = params.validationLenientMode ? params.validationLenientMode : false
        def Boolean failUnrecognisedParams = params.validationFailUnrecognisedParams ? params.validationFailUnrecognisedParams : false

        for (String specifiedParam in specifiedParamKeys) {
            // nextflow params
            if (NF_OPTIONS.contains(specifiedParam)) {
                errors << "You used a core Nextflow option with two hyphens: '--${specifiedParam}'. Please resubmit with '-${specifiedParam}'".toString()
            }
            // unexpected params
            def String schemaIgnoreParams = params.validationSchemaIgnoreParams
            def List params_ignore = schemaIgnoreParams ? schemaIgnoreParams.split(',') + 'schemaIgnoreParams' as List : []
            def expectedParamsLowerCase = expectedParams.collect{ it -> 
                def String p = it
                p.replace("-", "").toLowerCase() 
            }
            def specifiedParamLowerCase = specifiedParam.replace("-", "").toLowerCase()
            def isCamelCaseBug = (specifiedParam.contains("-") && !expectedParams.contains(specifiedParam) && expectedParamsLowerCase.contains(specifiedParamLowerCase))
            if (!expectedParams.contains(specifiedParam) && !params_ignore.contains(specifiedParam) && !isCamelCaseBug) {
                if (failUnrecognisedParams) {
                    errors << "* --${specifiedParam}: ${params[specifiedParam]}".toString()
                } else {
                    warnings << "* --${specifiedParam}: ${params[specifiedParam]}".toString()
                }
            }
        }

        //=====================================================================//
        // Validate parameters against the schema
        def String schema_string = Files.readString( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        final rawSchema = new JSONObject(new JSONTokener(schema_string))
        final SchemaLoader schemaLoader = SchemaLoader.builder()
                .schemaJson(rawSchema)
                .addFormatValidator("file-path", new FilePathValidator())
                .addFormatValidator("directory-path", new DirectoryPathValidator())
                .addFormatValidator("path", new PathValidator())
                .addFormatValidator("file-path-pattern", new FilePathPatternValidator())
                .build()
        final schema = schemaLoader.load().build()

        // check for warnings
        if( this.hasWarnings() ) {
            def msg = "The following invalid input values have been detected:\n\n" + this.getWarnings().join('\n').trim() + "\n\n"
            log.warn(msg)
        }

        // Colors
        def colors = logColours(useMonochromeLogs)

        // Validate
        try {
            if (lenientMode) {
                // Create new validator with LENIENT mode 
                Validator validator = Validator.builder()
                    .primitiveValidationStrategy(PrimitiveValidationStrategy.LENIENT)
                    .build();
                validator.performValidation(schema, paramsJSON);
            } else {
                schema.validate(paramsJSON)
            }
            if (this.hasErrors()) {
                // Needed when validationFailUnrecognisedParams is true
                def msg = "${colors.red}The following invalid input values have been detected:\n\n" + this.getErrors().join('\n').trim() + "\n${colors.reset}\n"
                log.error("ERROR: Validation of pipeline parameters failed!")
                throw new SchemaValidationException(msg, this.getErrors())
            }
        } catch (ValidationException e) {
            JSONObject exceptionJSON = (JSONObject) e.toJSON()
            collectErrors(exceptionJSON, paramsJSON, enums, rawSchema)
            def msg = "${colors.red}The following invalid input values have been detected:\n\n" + this.getErrors().join('\n').trim() + "\n${colors.reset}\n"
            log.error("ERROR: Validation of pipeline parameters failed!")
            throw new SchemaValidationException(msg, this.getErrors())
        }

        //=====================================================================//
        // Look for other schemas to validate
        for (group in schemaParams) {
            def Map properties = (Map) group.value['properties']
            for (p in properties) {
                def String key = (String) p.key
                if (!params[key]) {
                    continue
                }
                def Map property = properties[key] as Map
                if (property.containsKey('schema')) {
                    def String schema_name = getSchemaPath(baseDir, property['schema'].toString())
                    def Path file_path
                    try {
                        file_path = Nextflow.file(params[key]) as Path
                    } catch (IllegalArgumentException e) {
                        errors << "* --${key}: The file path '${params[key]}' is invalid. Unable to validate file.".toString()
                        continue
                    }
                    log.debug "Starting validation: '$key': '$file_path' with '$schema_name'"
                    def String fileType = SamplesheetConverter.getFileType(file_path)
                    def String delimiter = fileType == "csv" ? "," : fileType == "tsv" ? "\t" : null
                    def List<Map<String,String>> fileContent
                    def Map types = variableTypes(schema_name, baseDir)
                    def Boolean containsHeader = !(types.keySet().size() == 1 && types.keySet()[0] == "")

                    if(!containsHeader){
                        types = ["empty": types[""]]
                    }

                    if(fileType == "yaml"){
                        fileContent = new Yaml().load(file_path.text).collect {
                            if(containsHeader) {
                                return it as Map
                            }
                            return ["empty": it] as Map
                        }
                    }
                    else {
                        fileContent = file_path.splitCsv(header:containsHeader ?: ["empty"], strip:true, sep:delimiter, quote:'\"')
                    }
                    def List<Map<String,String>> fileContentCasted = castToType(fileContent, types)
                    if (validateFile(useMonochromeLogs, key, fileContentCasted, schema_name, baseDir, s3PathCheck)) {
                        log.debug "Validation passed: '$key': '$file_path' with '$schema_name'"
                    }
                }
            }
        }

        log.debug "Finishing parameters validation"
    }


    //
    // Function to obtain the variable types of properties from a JSON Schema
    //
    Map variableTypes(String schemaFilename, String baseDir) {
        def Map variableTypes = [:]
        def String type = ''

        // Read the schema
        def slurper = new JsonSlurper()
        def Map parsed = (Map) slurper.parse( Path.of(getSchemaPath(baseDir, schemaFilename)) )

        // Obtain the type of each variable in the schema
        def Map properties = (Map) parsed['items']['properties']
        for (p in properties) {
            def String key = (String) p.key
            def Map property = properties[key] as Map
            if (property.containsKey('type')) {
                if (property['type'] == 'number') {
                    type = 'float'
                }
                else {
                    type = property['type']
                }
                variableTypes[key] = type
            }
            else {
                variableTypes[key] = 'string' // If there isn't a type specifyed, return 'string' to avoid having a null value
            }
        }

        return variableTypes
    }


    //
    // Cast a value to the provided type in a Strict mode
    //
    Set<String> VALID_BOOLEAN_VALUES = ['true', 'false'] as Set

    List castToType(List<Map> rows, Map types) {
        def List<Map> casted = []

        for( Map row in rows) {
            def Map castedRow = [:]

            for (String key in row.keySet()) {
                def String str = row[key]
                def String type = types[key]

                try {
                    if( str == null || str == '' ) castedRow[key] = null
                    else if( type == null ) castedRow[key] = str
                    else if( type.toLowerCase() == 'boolean' && str.toLowerCase() in VALID_BOOLEAN_VALUES ) castedRow[key] = str.toBoolean()
                    else if( type.toLowerCase() == 'character' ) castedRow[key] = str.toCharacter()
                    else if( type.toLowerCase() == 'short' && str.isNumber() ) castedRow[key] = str.toShort()
                    else if( type.toLowerCase() == 'integer' && str.isInteger() ) castedRow[key] = str.toInteger()
                    else if( type.toLowerCase() == 'long' && str.isLong() ) castedRow[key] = str.toLong()
                    else if( type.toLowerCase() == 'float' && str.isFloat() ) castedRow[key] = str.toFloat()
                    else if( type.toLowerCase() == 'double' && str.isDouble() ) castedRow[key] = str.toDouble()
                    else if( type.toLowerCase() == 'string' ) castedRow[key] = str
                    else {
                        castedRow[key] = str
                    }
                } catch( Exception e ) {
                    log.warn "Unable to cast value $str to type $type: $e"
                    castedRow[key] = str
                }

            }

            casted = casted + castedRow
        }

        return casted
    }


    //
    // Function to validate a file by its schema
    //
    /* groovylint-disable-next-line UnusedPrivateMethodParameter */
    boolean validateFile(
        
        Boolean monochrome_logs, String paramName, Object fileContent, String schemaFilename, String baseDir, Boolean s3PathCheck = false
    
    ) {

        // Load the schema
        def String schema_string = Files.readString( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        final rawSchema = new JSONObject(new JSONTokener(schema_string))
        final SchemaLoader schemaLoader = SchemaLoader.builder()
            .schemaJson(rawSchema)
            .addFormatValidator("file-path", new FilePathValidator())
            .addFormatValidator("directory-path", new DirectoryPathValidator())
            .addFormatValidator("path", new PathValidator())
            .addFormatValidator("file-path-pattern", new FilePathPatternValidator())
            .build()
        final schema = schemaLoader.load().build()

        // Remove all null values from JSON object
        // and convert the groovy object to a JSONArray
        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()
        def JSONArray arrayJSON = new JSONArray(jsonGenerator.toJson(fileContent))

        //=====================================================================//
        // Check for params with expected values
        def slurper = new JsonSlurper()
        def Map parsed = (Map) slurper.parse( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        def Map schemaParams = (Map) ["items": parsed.get('items')] 

        // Collect expected parameters from the schema
        def enumsTuple = collectEnums(schemaParams)
        def List expectedParams = (List) enumsTuple[0] + addExpectedParams()
        def Map enums = (Map) enumsTuple[1]

        //=====================================================================//
        // Check if files or directories exist
        def List<String> pathsToCheck = (List) collectExists(schemaParams)
        pathsToCheck.each { String fieldName ->
            for (int i=0; i < arrayJSON.size(); i++) {
                def JSONObject entry = arrayJSON.getJSONObject(i)
                if ( entry.has(fieldName) ) {
                    pathExists(entry[fieldName].toString(), " Entry ${(i+1).toString()} - ${fieldName.toString()}", s3PathCheck)
                }
            }
        }

        //=====================================================================//
        // Validate
        try {
            // Create new validator with LENIENT mode 
            Validator validator = Validator.builder()
                .primitiveValidationStrategy(PrimitiveValidationStrategy.LENIENT)
                .build();
            validator.performValidation(schema, arrayJSON);
            if (this.hasErrors()) {
                // Needed for custom errors such as pathExists() errors
                def colors = logColours(monochrome_logs)
                def msg = "${colors.red}The following errors have been detected:\n\n" + this.getErrors().join('\n').trim() + "\n${colors.reset}\n"
                log.error("ERROR: Validation of '$paramName' file failed!")
                throw new SchemaValidationException(msg, this.getErrors())
            }
        } catch (ValidationException e) {
            def colors = logColours(monochrome_logs)
            JSONObject exceptionJSON = (JSONObject) e.toJSON()
            JSONObject objectJSON = new JSONObject();
            objectJSON.put("objects",arrayJSON);            
            collectErrors(exceptionJSON, objectJSON, enums, rawSchema)
            def msg = "${colors.red}The following errors have been detected:\n\n" + this.getErrors().join('\n').trim() + "\n${colors.reset}\n"
            log.error("ERROR: Validation of '$paramName' file failed!")
            throw new SchemaValidationException(msg, this.getErrors())
        }

        return true
    }


    //
    // Function to check if a file or directory exists
    //
    List pathExists(String path, String paramName, Boolean s3PathCheck) {
        if (path.startsWith('s3://') && !s3PathCheck) {
            log.debug "Ignoring validation of S3 URL path '${path}'".toString()
        } else {
            def Path file = Nextflow.file(path) as Path
            if (!file.exists()) {
                errors << "* --${paramName}: the file or directory '${path}' does not exist.".toString()
            }
        }
    }


    //
    // Function to collect parameters with an exists key in the schema.
    //
    List collectExists(Map schemaParams) {
        def exists = []
        for (group in schemaParams) {
            def Map properties = (Map) group.value['properties']
            for (p in properties) {
                def String key = (String) p.key
                def Map property = properties[key] as Map
                if (property.containsKey('exists') && property.containsKey('format')) {
                    if (property['exists'] && (property['format'] == 'file-path' || property['format'] == 'directory-path' || property['format'] == 'path') ) {
                        exists.push(key)
                    }
                }
            }
        }
        return exists
    }


    //
    // Function to collect enums (options) of a parameter and expected parameters (present in the schema)
    //
    Tuple collectEnums(Map schemaParams) {
        def expectedParams = []
        def enums = [:]
        for (group in schemaParams) {
            def Map properties = (Map) group.value['properties']
            for (p in properties) {
                def String key = (String) p.key
                expectedParams.push(key)
                def Map property = properties[key] as Map
                if (property.containsKey('enum')) {
                    enums[key] = property['enum']
                }
            }
        }
        return new Tuple (expectedParams, enums)
    }


    //
    // Wrap too long text
    //
    String wrapText(String text, Integer lineWidth, Integer indent) {
        List olines = []
        String oline = "" // " " * indent
        text.split(" ").each() { wrd ->
            if ((oline.size() + wrd.size()) <= lineWidth) {
                oline += wrd + " "
            } else {
                olines += oline
                oline = wrd + " "
            }
        }
        olines += oline
        return olines.join("\n" + " " * indent)
    }

    //
    // Beautify parameters for --help
    //
    @Function
    String paramsHelp(
        Map options = null,
        String command
    ) {
        def Map params = initialiseExpectedParams(session.params)
        def String baseDir = session.baseDir

        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'
        def Boolean useMonochromeLogs = options?.containsKey('monochrome_logs') ? options.monochrome_logs as Boolean : 
            params.monochrome_logs ? params.monochrome_logs as Boolean : 
            params.monochromeLogs  ? params.monochromeLogs as Boolean :
            false

        def colors = logColours(useMonochromeLogs)
        Integer num_hidden = 0
        String output  = ''
        output        += 'Typical pipeline command:\n\n'
        output        += "  ${colors.cyan}${command}${colors.reset}\n\n"
        Map params_map = paramsLoad( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        Integer max_chars  = paramsMaxChars(params_map) + 1
        Integer desc_indent = max_chars + 14
        Integer dec_linewidth = 160 - desc_indent

        // If a value is passed to help
        if (params.help instanceof String) {
            def String param = params.help
            def Map get_param = [:]
            for (group in params_map.keySet()) {
                def Map group_params = params_map.get(group) as Map // This gets the parameters of that particular group
                if (group_params.containsKey(param)) {
                    get_param = group_params.get(param) as Map 
                }
            }
            if (!get_param) {
                throw new Exception("Specified param '${param}' does not exist in JSON schema.")
            }
            output += "--" + param + '\n'
            for (property in get_param) {
                if (property.key == "fa_icon") {
                    continue;
                }
                def String key = property.key
                def String value = property.value
                def Integer lineWidth = 160 - 17
                def Integer indent = 17
                if (value.length() > lineWidth) {
                    value = wrapText(value, lineWidth, indent)
                }
                output += "    " + colors.dim + key.padRight(11) + ": " + colors.reset + value + '\n'
            }
            output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
            return output
        }

        for (group in params_map.keySet()) {
            Integer num_params = 0
            String group_output = "$colors.underlined$colors.bold$group$colors.reset\n"
            def Map group_params = params_map.get(group) as Map // This gets the parameters of that particular group
            for (String param in group_params.keySet()) {
                def Map get_param = group_params.get(param) as Map 
                def String type = '[' + get_param.type + ']'
                def String enums_string = ""
                if (get_param.enum != null) {
                    def List enums = (List) get_param.enum
                    def String chop_enums = enums.join(", ")
                    if(chop_enums.length() > dec_linewidth){
                        chop_enums = chop_enums.substring(0, dec_linewidth-5)
                        chop_enums = chop_enums.substring(0, chop_enums.lastIndexOf(",")) + ", ..."
                    }
                    enums_string = " (accepted: " + chop_enums + ")"
                }
                def String description = get_param.description
                def defaultValue = get_param.default != null ? " [default: " + get_param.default.toString() + "]" : ''
                def description_default = description + colors.dim + enums_string + defaultValue + colors.reset
                // Wrap long description texts
                // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
                if (description_default.length() > dec_linewidth){
                    description_default = wrapText(description_default, dec_linewidth, desc_indent)
                }
                if (get_param.hidden && !params.validationShowHiddenParams) {
                    num_hidden += 1
                    continue;
                }
                group_output += "  --" +  param.padRight(max_chars) + colors.dim + type.padRight(10) + colors.reset + description_default + '\n'
                num_params += 1
            }
            group_output += '\n'
            if (num_params > 0){
                output += group_output
            }
        }
        if (num_hidden > 0){
            output += "$colors.dim !! Hiding $num_hidden params, use --validationShowHiddenParams to show them !!\n$colors.reset"
        }
        output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
        return output
    }

    //
    // Groovy Map summarising parameters/workflow options used by the pipeline
    //
    @Function
    public LinkedHashMap paramsSummaryMap(
        Map options = null,
        WorkflowMetadata workflow
        ) {
        
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'
        def String baseDir = session.baseDir
        def Map params = session.params
        
        // Get a selection of core Nextflow workflow options
        def Map workflow_summary = [:]
        if (workflow.revision) {
            workflow_summary['revision'] = workflow.revision
        }
        workflow_summary['runName']      = workflow.runName
        if (workflow.containerEngine) {
            workflow_summary['containerEngine'] = workflow.containerEngine
        }
        if (workflow.container) {
            workflow_summary['container'] = workflow.container
        }
        def String configFiles = workflow.configFiles
        workflow_summary['launchDir']    = workflow.launchDir
        workflow_summary['workDir']      = workflow.workDir
        workflow_summary['projectDir']   = workflow.projectDir
        workflow_summary['userName']     = workflow.userName
        workflow_summary['profile']      = workflow.profile
        workflow_summary['configFiles']  = configFiles.join(', ')

        // Get pipeline parameters defined in JSON Schema
        def Map params_summary = [:]
        def Map params_map = paramsLoad( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        for (group in params_map.keySet()) {
            def sub_params = new LinkedHashMap()
            def Map group_params = params_map.get(group)  as Map // This gets the parameters of that particular group
            for (String param in group_params.keySet()) {
                if (params.containsKey(param)) {
                    def String params_value = params.get(param)
                    def Map group_params_value = group_params.get(param) as Map 
                    def String schema_value = group_params_value.default
                    def String param_type   = group_params_value.type
                    if (schema_value != null) {
                        if (param_type == 'string') {
                            if (schema_value.contains('$projectDir') || schema_value.contains('${projectDir}')) {
                                def sub_string = schema_value.replace('\$projectDir', '')
                                sub_string     = sub_string.replace('\${projectDir}', '')
                                if (params_value.contains(sub_string)) {
                                    schema_value = params_value
                                }
                            }
                            if (schema_value.contains('$params.outdir') || schema_value.contains('${params.outdir}')) {
                                def sub_string = schema_value.replace('\$params.outdir', '')
                                sub_string     = sub_string.replace('\${params.outdir}', '')
                                if ("${params.outdir}${sub_string}" == params_value) {
                                    schema_value = params_value
                                }
                            }
                        }
                    }

                    // We have a default in the schema, and this isn't it
                    if (schema_value != null && params_value != schema_value) {
                        sub_params.put(param, params_value)
                    }
                    // No default in the schema, and this isn't empty or false
                    else if (schema_value == null && params_value != "" && params_value != null && params_value != false && params_value != 'false') {
                        sub_params.put(param, params_value)
                    }
                }
            }
            params_summary.put(group, sub_params)
        }
        return [ 'Core Nextflow options' : workflow_summary ] << params_summary as LinkedHashMap
    }

    //
    // Beautify parameters for summary and return as string
    //
    @Function
    public String paramsSummaryLog(
        Map options = null,
        WorkflowMetadata workflow
    ) {

        def String baseDir = session.baseDir
        def Map params = session.params

        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'
        def Boolean useMonochromeLogs = options?.containsKey('monochrome_logs') ? options.monochrome_logs as Boolean :
            params.monochrome_logs ? params.monochrome_logs as Boolean : 
            params.monochromeLogs  ? params.monochromeLogs as Boolean :
            false

        def colors = logColours(useMonochromeLogs)
        String output  = ''
        def LinkedHashMap params_map = paramsSummaryMap(workflow, parameters_schema: schemaFilename)
        def max_chars  = paramsMaxChars(params_map)
        for (group in params_map.keySet()) {
            def Map group_params = params_map.get(group) as Map // This gets the parameters of that particular group
            if (group_params) {
                output += "$colors.bold$group$colors.reset\n"
                for (String param in group_params.keySet()) {
                    output += "  " + colors.blue + param.padRight(max_chars) + ": " + colors.green +  group_params.get(param) + colors.reset + '\n'
                }
                output += '\n'
            }
        }
        output += "!! Only displaying parameters that differ from the pipeline defaults !!\n"
        output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
        return output
    }

    //
    // Loop over nested exceptions and print the causingException
    //
    private void collectErrors(JSONObject exJSON, JSONObject paramsJSON, Map enums, JSONObject schemaJSON, Integer limit=5) {
        def JSONArray causingExceptions = (JSONArray) exJSON['causingExceptions']
        def JSONArray valuesJSON = new JSONArray ()
        def String validationType = "parameter: --"
        if (paramsJSON.has('objects')) {
            valuesJSON = (JSONArray) paramsJSON['objects']
            validationType = "value: "
        } 
        def Integer entryNumber = 0
        if (causingExceptions.length() == 0) {
            def String pointer = (String) exJSON['pointerToViolation'] - ~/^#\//
            def String message = (String) exJSON['message']
            def Pattern p = (Pattern) ~/required key \[([^\]]+)\] not found/
            def Matcher m = message =~ p
            // Missing required param
            if(m.matches()){
                def List l = m[0] as ArrayList
                if (pointer.isNumber()) {
                    entryNumber = pointer.replace('/', ' - ') as Integer
                    entryNumber = entryNumber + 1
                    errors << "* -- Entry ${entryNumber}: Missing required ${validationType}${l[1]}".toString()
                } else {
                    errors << "* Missing required ${validationType}${l[1]}".toString()
                }
            }
            // Other base-level error
            else if(exJSON['pointerToViolation'] == '#'){
                errors << "* ${message}".toString()
            }
            // Error with specific param
            else {
                def String param = (String) exJSON['pointerToViolation'] - ~/^#\//
                def String paramName = param
                def param_val = ""
                if (paramsJSON.has('objects')) {
                    def paramSplit = param.tokenize( '/' )
                    int indexInt = paramSplit[0] as int
                    String paramString = paramSplit[1] as String
                    paramName = paramString
                    param_val = valuesJSON[indexInt][paramString].toString()
                } else {
                    param_val = paramsJSON[param].toString()
                }
                if (enums.containsKey(param)) {
                    def error_msg = "* --${param}: '${param_val}' is not a valid choice (Available choices"
                    def List enums_param = (List) enums[param]
                    if (enums_param.size() > limit) {
                        errors << "${error_msg} (${limit} of ${enums_param.size()}): ${enums_param[0..limit-1].join(', ')}, ... )".toString()
                    } else {
                        errors << "${error_msg}: ${enums_param.join(', ')})".toString()
                    }
                } else {
                    if (param.contains('/')) {
                        entryNumber = param.split('/')[0] as Integer
                        entryNumber = entryNumber + 1
                        def String columnName = param.split('/')[1]
                        paramName = columnName
                        param = " Entry ${entryNumber} - ${columnName}"
                    }
                    // Custom errorMessage
                    def String errorMessage
                    try {
                        errorMessage = schemaJSON['items']['properties'][paramName]['errorMessage']
                    } catch (JSONException) {
                        def Map paramMap = findDeep(schemaJSON.toMap(), paramName) as Map
                        errorMessage = paramMap['errorMessage']
                    }
                    if (errorMessage) {
                        log.debug "* --${param}: ${message} (${param_val})".toString()
                        message = errorMessage
                    }
                    errors << "* --${param}: ${message} (${param_val})".toString()
                }
            }
            errors.unique()
        }
        for (ex in causingExceptions) {
            def JSONObject exception = (JSONObject) ex
            collectErrors(exception, paramsJSON, enums, schemaJSON)
        }
    }

    //
    // Remove an element from a JSONArray
    //
    private static JSONArray removeElement(JSONArray json_array, String element) {
        def list = []
        int len = json_array.length()
        for (int i=0;i<len;i++){
            list.add(json_array.get(i).toString())
        }
        list.remove(element)
        JSONArray jsArray = new JSONArray(list)
        return jsArray
    }

    //
    // Clean and check parameters relative to Nextflow native classes
    //
    private static Map cleanParameters(Map params) {
        def Map new_params = (Map) params.getClass().newInstance(params)
        for (p in params) {
            // remove anything evaluating to false
            if (!p['value']) {
                new_params.remove(p.key)
            }
            // Cast MemoryUnit to String
            if (p['value'] instanceof MemoryUnit) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Cast Duration to String
            if (p['value'] instanceof Duration) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Cast LinkedHashMap to String
            if (p['value'] instanceof LinkedHashMap) {
                new_params.replace(p.key, p['value'].toString())
            }
        }
        return new_params
    }

    //
    // This function tries to read a JSON params file
    //
    private static LinkedHashMap paramsLoad(Path json_schema) {
        def params_map = new LinkedHashMap()
        try {
            params_map = paramsRead(json_schema)
        } catch (Exception e) {
            println "Could not read parameters settings from JSON. $e"
            params_map = new LinkedHashMap()
        }
        return params_map
    }

    //
    // Method to actually read in JSON file using Groovy.
    // Group (as Key), values are all parameters
    //    - Parameter1 as Key, Description as Value
    //    - Parameter2 as Key, Description as Value
    //    ....
    // Group
    //    -
    private static LinkedHashMap paramsRead(Path json_schema) throws Exception {
        def slurper = new JsonSlurper()
        def Map schema = (Map) slurper.parse( json_schema )
        def Map schema_definitions = (Map) schema.get('definitions')
        def Map schema_properties = (Map) schema.get('properties')
        /* Tree looks like this in nf-core schema
        * definitions <- this is what the first get('definitions') gets us
                group 1
                    title
                    description
                        properties
                        parameter 1
                            type
                            description
                        parameter 2
                            type
                            description
                group 2
                    title
                    description
                        properties
                        parameter 1
                            type
                            description
        * properties <- parameters can also be ungrouped, outside of definitions
                parameter 1
                    type
                    description
        */

        def params_map = new LinkedHashMap()
        // Grouped params
        if (schema_definitions) {
            for (group in schema_definitions) {
                def Map group_property = (Map) group.value['properties'] // Gets the property object of the group
                def String title = (String) group.value['title']
                def sub_params = new LinkedHashMap()
                group_property.each { innerkey, value ->
                    sub_params.put(innerkey, value)
                }
                params_map.put(title, sub_params)
            }
        }

        // Ungrouped params
        if (schema_properties) {
            def ungrouped_params = new LinkedHashMap()
            schema_properties.each { innerkey, value ->
                ungrouped_params.put(innerkey, value)
            }
            params_map.put("Other parameters", ungrouped_params)
        }

        return params_map
    }

    //
    // Get maximum number of characters across all parameter names
    //
    private static Integer paramsMaxChars( Map params_map) {
        Integer max_chars = 0
        for (group in params_map.keySet()) {
            def Map group_params = (Map) params_map.get(group)  // This gets the parameters of that particular group
            for (String param in group_params.keySet()) {
                if (param.size() > max_chars) {
                    max_chars = param.size()
                }
            }
        }
        return max_chars
    }

    //
    // ANSII Colours used for terminal logging
    //
    private static Map logColours(Boolean monochrome_logs) {
        Map colorcodes = [:]

        // Reset / Meta
        colorcodes['reset']      = monochrome_logs ? '' : "\033[0m"
        colorcodes['bold']       = monochrome_logs ? '' : "\033[1m"
        colorcodes['dim']        = monochrome_logs ? '' : "\033[2m"
        colorcodes['underlined'] = monochrome_logs ? '' : "\033[4m"
        colorcodes['blink']      = monochrome_logs ? '' : "\033[5m"
        colorcodes['reverse']    = monochrome_logs ? '' : "\033[7m"
        colorcodes['hidden']     = monochrome_logs ? '' : "\033[8m"

        // Regular Colors
        colorcodes['black']      = monochrome_logs ? '' : "\033[0;30m"
        colorcodes['red']        = monochrome_logs ? '' : "\033[0;31m"
        colorcodes['green']      = monochrome_logs ? '' : "\033[0;32m"
        colorcodes['yellow']     = monochrome_logs ? '' : "\033[0;33m"
        colorcodes['blue']       = monochrome_logs ? '' : "\033[0;34m"
        colorcodes['purple']     = monochrome_logs ? '' : "\033[0;35m"
        colorcodes['cyan']       = monochrome_logs ? '' : "\033[0;36m"
        colorcodes['white']      = monochrome_logs ? '' : "\033[0;37m"

        // Bold
        colorcodes['bblack']     = monochrome_logs ? '' : "\033[1;30m"
        colorcodes['bred']       = monochrome_logs ? '' : "\033[1;31m"
        colorcodes['bgreen']     = monochrome_logs ? '' : "\033[1;32m"
        colorcodes['byellow']    = monochrome_logs ? '' : "\033[1;33m"
        colorcodes['bblue']      = monochrome_logs ? '' : "\033[1;34m"
        colorcodes['bpurple']    = monochrome_logs ? '' : "\033[1;35m"
        colorcodes['bcyan']      = monochrome_logs ? '' : "\033[1;36m"
        colorcodes['bwhite']     = monochrome_logs ? '' : "\033[1;37m"

        // Underline
        colorcodes['ublack']     = monochrome_logs ? '' : "\033[4;30m"
        colorcodes['ured']       = monochrome_logs ? '' : "\033[4;31m"
        colorcodes['ugreen']     = monochrome_logs ? '' : "\033[4;32m"
        colorcodes['uyellow']    = monochrome_logs ? '' : "\033[4;33m"
        colorcodes['ublue']      = monochrome_logs ? '' : "\033[4;34m"
        colorcodes['upurple']    = monochrome_logs ? '' : "\033[4;35m"
        colorcodes['ucyan']      = monochrome_logs ? '' : "\033[4;36m"
        colorcodes['uwhite']     = monochrome_logs ? '' : "\033[4;37m"

        // High Intensity
        colorcodes['iblack']     = monochrome_logs ? '' : "\033[0;90m"
        colorcodes['ired']       = monochrome_logs ? '' : "\033[0;91m"
        colorcodes['igreen']     = monochrome_logs ? '' : "\033[0;92m"
        colorcodes['iyellow']    = monochrome_logs ? '' : "\033[0;93m"
        colorcodes['iblue']      = monochrome_logs ? '' : "\033[0;94m"
        colorcodes['ipurple']    = monochrome_logs ? '' : "\033[0;95m"
        colorcodes['icyan']      = monochrome_logs ? '' : "\033[0;96m"
        colorcodes['iwhite']     = monochrome_logs ? '' : "\033[0;97m"

        // Bold High Intensity
        colorcodes['biblack']    = monochrome_logs ? '' : "\033[1;90m"
        colorcodes['bired']      = monochrome_logs ? '' : "\033[1;91m"
        colorcodes['bigreen']    = monochrome_logs ? '' : "\033[1;92m"
        colorcodes['biyellow']   = monochrome_logs ? '' : "\033[1;93m"
        colorcodes['biblue']     = monochrome_logs ? '' : "\033[1;94m"
        colorcodes['bipurple']   = monochrome_logs ? '' : "\033[1;95m"
        colorcodes['bicyan']     = monochrome_logs ? '' : "\033[1;96m"
        colorcodes['biwhite']    = monochrome_logs ? '' : "\033[1;97m"

        return colorcodes
    }
}
