package nextflow.validation

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import org.yaml.snakeyaml.Yaml

import nextflow.Channel
import nextflow.Global
import nextflow.Nextflow
import nextflow.plugin.extension.Function
import nextflow.Session


@Slf4j
@CompileStatic
class SamplesheetConverter {

    private static List<String> errors = []
    private static List<String> schemaErrors = []
    private static List<String> warnings = []

    static boolean hasErrors() { errors.size()>0 || schemaErrors.size()>0 }
    static Set<String> getErrors() { errors.collect { "[Samplesheet Error] ${it}".toString() } as Set }
    static Set<String> getSchemaErrors() { schemaErrors.collect { "[Samplesheet Schema Error] ${it}".toString() } as Set }

    static boolean hasWarnings() { warnings.size()>0 }
    static Set<String> getWarnings() { warnings.collect { "[Samplesheet Warning] ${it}".toString() } as Set }

    private static Integer sampleCount = 0

    static resetCount(){ sampleCount = 0 }
    static increaseCount(){ sampleCount++ }
    static Integer getCount(){ sampleCount }

    static List convertToList(
        Path samplesheetFile, 
        Path schemaFile
        ) {

        def Map schema = (Map) new JsonSlurper().parseText(schemaFile.text)
        def Map<String, Map<String, String>> schemaFields = schema["properties"]
        def Set<String> allFields = schemaFields.keySet()
        def List<String> requiredFields = schema["required"]

        def String fileType = getFileType(samplesheetFile)
        def String delimiter = fileType == "csv" ? "," : fileType == "tsv" ? "\t" : null
        def List<Map<String,String>> samplesheetList

        if(fileType == "yaml"){
            samplesheetList = new Yaml().load((samplesheetFile.text))
        }
        else {
            Path fileSamplesheet = Nextflow.file(samplesheetFile) as Path
            samplesheetList = fileSamplesheet.splitCsv(header:true, strip:true, sep:delimiter)
        }

        // Field checks + returning the channels
        def Map<String,List<String>> booleanUniques = [:]
        def Map<String,List<Map<String,String>>> listUniques = [:]
        def Boolean headerCheck = true
        resetCount()
        
        def List outputs = samplesheetList.collect { Map row ->
            increaseCount()
            def Set rowKeys = row.keySet()
            def Set differences = allFields - rowKeys
            def String yamlInfo = fileType == "yaml" ? " for sample ${this.getCount()}." : ""

            // Check the header (CSV/TSV) or present fields (YAML)
            if(headerCheck) {
                def unexpectedFields = rowKeys - allFields
                if(unexpectedFields.size() > 0) {
                    this.warnings << "The samplesheet contains following unchecked field(s): ${unexpectedFields}${yamlInfo}".toString()
                }

                def List<String> missingFields = requiredFields - rowKeys
                if(missingFields.size() > 0) {
                    this.errors << "The samplesheet requires '${requiredFields.join(",")}' as header field(s), but is missing these: ${missingFields}${yamlInfo}".toString()
                }

                if(fileType != 'yaml'){
                    headerCheck = false
                }
            }

            def Map meta = [:]
            def ArrayList output = []

            for( Map.Entry<String, Map> field : schemaFields ){
                def String key = field.key
                def String input = row[key]

                // Check if the required fields exist
                if((input == null || input == "") && key in requiredFields){
                    this.errors << "Sample ${this.getCount()} does not contain an input for required field '${key}'.".toString()
                }

                // Check if the field is deprecated
                if(field['value']['deprecated']){
                    this.warnings << "The '${key}' field is deprecated and will no longer be used in the future. Please check the official documentation of the pipeline for more information.".toString()
                }

                // Check required dependencies
                def List<String> dependencies = field['value']["dependentRequired"] as List<String>
                if(input && dependencies) {
                    def List<String> missingValues = []
                    for( dependency in dependencies ){
                        if(row[dependency] == "" || !(row[dependency])) {
                            missingValues.add(dependency)
                        }
                    }
                    if (missingValues) {
                        this.errors << "${dependencies} field(s) should be defined when '${key}' is specified, but  the field(s) ${missingValues} is/are not defined.".toString()
                    }
                }
                
                // Check if the field is unique
                def unique = field['value']['unique']
                def Boolean uniqueIsList = unique instanceof ArrayList
                if(unique && !uniqueIsList){
                    if(!(key in booleanUniques)){
                        booleanUniques[key] = []
                    }
                    if(input in booleanUniques[key] && input){
                        this.errors << "The '${key}' value needs to be unique. '${input}' was found at least twice in the samplesheet.".toString()
                    }
                    booleanUniques[key].add(input)
                }
                else if(unique && uniqueIsList) {
                    def Map<String,String> newMap = row.subMap([key] + (List) unique)
                    if(!(key in listUniques)){
                        listUniques[key] = []
                    }
                    if(newMap in listUniques[key] && input){
                        this.errors << "The combination of '${key}' with fields ${unique} needs to be unique. ${newMap} was found at least twice.".toString()
                    }
                    listUniques[key].add(newMap)
                }

                // Check enumeration
                def List enumeration = field['value']['enum'] as List
                if(input && enumeration && !(enumeration.contains(input))){
                    this.errors << "The '${key}' value for sample ${this.getCount()} needs to be one of ${enumeration}, but is '${input}'.".toString()
                }

                // Convert field to a meta field or add it as an input to the channel
                def String metaNames = field['value']['meta']
                if(metaNames) {
                    for(name : metaNames.tokenize(',')) {
                        meta[name] = (input != '' && input) ? 
                                checkAndTransform(input, field) : 
                            field['value']['default'] ? 
                                checkAndTransform(field['value']['default'] as String, field) : 
                                null
                    }
                }
                else {
                    def inputFile = (input != '' && input) ? 
                            checkAndTransform(input, field) : 
                        field['value']['default'] ? 
                            checkAndTransform(field['value']['default'] as String, field) : 
                            []
                    output.add(inputFile)
                }
            }
            output.add(0, meta)
            return output
        }

        // check for errors
        if (this.hasErrors()) {
            String message = this.getErrors().join("\n").trim() + this.getSchemaErrors().join("\n").trim()
            throw new SchemaValidationException(message, (this.getErrors() + this.getSchemaErrors()) as List)
        }

        // check for warnings
        if( this.hasWarnings() ) {
            def msg = this.getWarnings().join('\n').trim()
            log.warn(msg)
        }

        return outputs
    }

    // Function to infer the file type of the samplesheet
    private static String getFileType(
        Path samplesheetFile
    ) {
        def String extension = samplesheetFile.getExtension()
        if (extension in ["csv", "tsv", "yml", "yaml"]) {
            return extension == "yml" ? "yaml" : extension
        }

        def String header = getHeader(samplesheetFile)

        def Integer commaCount = header.count(",")
        def Integer tabCount = header.count("\t")

        if ( commaCount == tabCount ){
            this.errors << "Could not derive file type from ${samplesheetFile}. Please specify the file extension (CSV, TSV, YML and YAML are supported).".toString()
        }
        if ( commaCount > tabCount ){
            return "csv"
        }
        else {
            return "tsv"
        }
    }

    // Function to get the header from a CSV or TSV file
    private static String getHeader(
        Path samplesheetFile
    ) {
        def String header
        samplesheetFile.withReader { header = it.readLine() }
        return header
    }

    // Function to check and transform an input field from the samplesheet
    private static checkAndTransform(
        String input,
        Map.Entry<String, Map> field
    ) {
        def String type = field['value']['type']
        def String key = field.key

        def List<String> supportedTypes = ["string", "integer", "boolean", "number"]
        if(!(type in supportedTypes)) {
            this.schemaErrors << "The type '${type}' specified for ${key} is not supported. Please specify one of these instead: ${supportedTypes}".toString()
        }

        // Check and convert string values
        if(type == "string" || !type) {
            def String result = input as String
            
            // Check the regex pattern
            def String regexPattern = field['value']['pattern'] && field['value']['pattern'] != '' ? field['value']['pattern'] : '^.*$'
            if(!(result ==~ regexPattern) && result != '') {
                this.errors << "The '${key}' value (${result}) for sample ${this.getCount()} does not match the pattern '${regexPattern}'.".toString()
            }

            // Check the inclusive maximum length
            def Integer maxLength = field['value']['maxLength'] as Integer
            if(maxLength && result.size() > maxLength){
                this.errors << "The '${key}' value (${result}) for sample ${this.getCount()} does contains more characters than the maximum amount of ${maxLength}.".toString()
            }

            // Check the inclusive minimum length
            def Integer minLength = field['value']['minLength'] as Integer
            if(minLength && result.size() < minLength){
                this.errors << "The '${key}' value (${result}) for sample ${this.getCount()} does contains less characters than the minimum amount of ${minLength}.".toString()
            }
            
            // Check and convert to the desired format
            def String format = field['value']['format']
            List<String> supportedFormats = ["file-path", "directory-path"]
            if(format && !(format in supportedFormats)) {
                this.schemaErrors << "The string format '${format}' specified for ${key} is not supported. Please specify one of these instead: ${supportedFormats} or don't supply a format for a simple string.".toString()
            }
            else if(format && (format == "file-path" || format =="directory-path")) {
                def Path inputFile = Nextflow.file(input) as Path
                if(!inputFile.exists()){
                    this.errors << "The '${key}' file or directory (${input}) for sample ${this.getCount()} does not exist.".toString()
                }
                return inputFile
            }

            // Return the plain string value (if no format is supplied)
            return result

        }

        // Check and convert integer values
        else if(type == "integer" || type == "number") {

            // Convert the string value to an integer value
            def Integer result
            try {
                result = input as Integer
            } catch(java.lang.NumberFormatException e) {
                this.errors << "The '${key}' value (${input}) for sample ${this.getCount()} is not a valid integer.".toString()
            }

            // Check if the value is a multiple of the integer specified in multipleOf
            def Integer multipleOf = field['value']['multipleOf'] as Integer
            if(multipleOf && result % multipleOf != 0){
                this.errors << "The '${key}' value (${input}) for sample ${this.getCount()} is not a multiple of ${multipleOf}.".toString()
            }

            // Check the inclusive maximum value
            def Integer maximum = field['value']['maximum'] as Integer
            if(maximum && result > maximum){
                this.errors << "The '${key}' value (${input}) for sample ${this.getCount()} is above the maximum amount of ${maximum}.".toString()
            }

            // Check the inclusive minimum value
            def Integer minimum = field['value']['minimum'] as Integer
            if(minimum && result < minimum){
                this.errors << "The '${key}' value (${input}) for sample ${this.getCount()} is below the minimum amount of ${minimum}.".toString()
            }

            // Return the integer
            return result
        }

        // Check and convert boolean values
        else if(type == "boolean") {

            // Convert and return the boolean value
            if(input.toLowerCase() == "true") {
                return true
            }
            else if(input.toLowerCase() == "false") {
                return false
            }
            else {
                this.errors << "The '${key}' value (${input}) for sample ${this.getCount()} is not a valid boolean.".toString()
            }
        }
    }
}