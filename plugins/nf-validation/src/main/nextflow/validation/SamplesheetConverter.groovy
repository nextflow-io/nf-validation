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
    static List<String> getErrors() { errors.collect { "[Samplesheet Error] ${it}".toString() } }
    static List<String> getSchemaErrors() { schemaErrors.collect { "[Samplesheet Schema Error] ${it}".toString() } }

    static boolean hasWarnings() { warnings.size()>0 }
    static List<String> getWarnings() { warnings.collect { "[Samplesheet Warning] ${it}".toString() } }

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
        def Map<String,List<String>> uniques = [:]
        def Boolean headerCheck = true
        resetCount()
        
        def List outputs = samplesheetList.collect { Map row ->
            increaseCount()
            def Set rowKeys = row.keySet()
            def Set differences = allFields - rowKeys
            def String yamlInfo = fileType == "yaml" ? " for sample ${this.getCount()}." : ""

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

            // Check required dependencies
            def Map dependencies = schema["dependentRequired"]
            if(dependencies) {
                for( dependency in dependencies ){
                    if(row[dependency.key] != "" && row[dependency.key]) {
                        def List<String> missingValues = []
                        for( String value in dependency.value ){
                            if(row[value] == "" || !(row[value])) {
                                missingValues.add(value)
                            }
                        }
                        if (missingValues) {
                            this.errors << "${dependency.value} field(s) should be defined when '${dependency.key}' is specified, but  the field(s) ${missingValues} are/is not defined.".toString()
                        }
                    }
                }
            }

            def Map meta = [:]
            def ArrayList output = []

            for( Map.Entry<String, Map> field : schemaFields ){
                def String key = field.key
                def String metaNames = field['value']['meta']

                def String input = row[key]

                if((input == null || input == "") && key in requiredFields){
                    this.errors << "Sample ${this.getCount()} does not contain an input for required field '${key}'.".toString()
                }
                else if(field['value']['unique']){
                    if(!(key in uniques)){
                        uniques[key] = []
                    }
                    if(input in uniques[key] && input){
                        this.errors << "The '${key}' value needs to be unique. '${input}' was found twice in the samplesheet.".toString()
                    }
                    uniques[key].add(input)
                }

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
            throw new SchemaValidationException(message, this.getErrors())
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
        def String format = field['value']['format']
        def String key = field.key
        def String regexPattern = field['value']['pattern'] && field['value']['pattern'] != '' ? field['value']['pattern'] : '^.*$'

        def List<String> supportedTypes = ["string", "integer", "boolean"]
        if(!(type in supportedTypes)) {
            this.schemaErrors << "The type '${type}' specified for ${key} is not supported. Please specify one of these instead: ${supportedTypes}".toString()
        }

        if(type == "string" || !type) {
            if(!(input ==~ regexPattern) && input != '' && input) {
                this.errors << "The '${key}' value for sample ${this.getCount()} does not match the pattern '${regexPattern}'.".toString()
            }
            List<String> supportedFormats = ["file-path", "directory-path"]
            if(!(format in supportedFormats) && format) {
                this.schemaErrors << "The string format '${format}' specified for ${key} is not supported. Please specify one of these instead: ${supportedFormats} or don't supply a format for a simple string.".toString()
            }
            if(format == "file-path" || format =="directory-path") {
                def Path inputFile = Nextflow.file(input) as Path
                if(!inputFile.exists()){
                    this.errors << "The '${key}' file or directory (${input}) for sample ${this.getCount()} does not exist.".toString()
                }
                return inputFile
            }
            else {
                return input as String
            }
        }
        else if(type == "integer") {
            try {
                return input as Integer
            } catch(java.lang.NumberFormatException e) {
                this.errors << "The '${key}' value (${input}) for sample ${this.getCount()} is not a valid integer.".toString()
            }
        }
        else if(type == "boolean") {
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