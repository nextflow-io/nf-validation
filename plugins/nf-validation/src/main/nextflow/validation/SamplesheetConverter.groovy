package nextflow.validation

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel

import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

import org.yaml.snakeyaml.Yaml
import org.everit.json.schema.loader.SchemaLoader
import org.everit.json.schema.PrimitiveValidationStrategy
import org.everit.json.schema.ValidationException
import org.everit.json.schema.SchemaException
import org.everit.json.schema.Schema
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

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

    private static List<Map> rows = []

    static boolean hasErrors() { errors.size()>0 }
    static Set<String> getErrors() { errors.sort().collect { "\t${it}".toString() } as Set }

    static boolean hasSchemaErrors() { schemaErrors.size()>0 }
    static Set<String> getSchemaErrors() { schemaErrors.sort().collect { "\t${it}".toString() } as Set }

    static boolean hasWarnings() { warnings.size()>0 }
    static Set<String> getWarnings() { warnings.sort().collect { "\t${it}".toString() } as Set }

    private static Integer sampleCount = 0

    static resetCount(){ sampleCount = 0 }
    static increaseCount(){ sampleCount++ }
    static Integer getCount(){ sampleCount }


    static List convertToList(
        Path samplesheetFile, 
        Path schemaFile,
        Boolean skipDuplicateCheck
        ) {

        def Map schemaMap = (Map) new JsonSlurper().parseText(schemaFile.text)
        def Map<String, Map<String, String>> schemaFields = (Map) schemaMap["items"]["properties"]
        def Set<String> allFields = schemaFields.keySet()
        def List<String> requiredFields = (List) schemaMap["items"]["required"]
        def Boolean containsHeader = !(allFields.size() == 1 && allFields[0] == "")

        def String fileType = getFileType(samplesheetFile)
        def String delimiter = fileType == "csv" ? "," : fileType == "tsv" ? "\t" : null
        def List<Map<String,String>> samplesheetList

        if(fileType == "yaml"){
            samplesheetList = new Yaml().load((samplesheetFile.text)).collect {
                if(containsHeader) {
                    return it as Map
                }
                return ["empty": it] as Map
            }
        }
        else {
            Path fileSamplesheet = Nextflow.file(samplesheetFile) as Path
            samplesheetList = fileSamplesheet.splitCsv(header:containsHeader ?: ["empty"], strip:true, sep:delimiter, quote:'\"')
        }

        // Field checks + returning the channels
        def Map<String,List<String>> booleanUniques = [:]
        def Map<String,List<Map<String,String>>> listUniques = [:]
        def Boolean headerCheck = true
        this.rows = []
        resetCount()
        def List outputs = samplesheetList.collect { Map<String,String> fullRow ->
            increaseCount()

            Map<String,String> row = fullRow.findAll { it.value != "" }
            def Set rowKeys = containsHeader ? row.keySet() : ["empty"].toSet()
            def String yamlInfo = fileType == "yaml" ? " for entry ${this.getCount()}." : ""

            // Check the header (CSV/TSV) or present fields (YAML)
            if(headerCheck) {
                def unexpectedFields = containsHeader ? rowKeys - allFields : []
                if(unexpectedFields.size() > 0) {
                    this.warnings << "The samplesheet contains following unchecked field(s): ${unexpectedFields}${yamlInfo}".toString()
                }

                if(fileType != 'yaml'){
                    headerCheck = false
                }
            }

            // Check for row uniqueness
            if(!skipDuplicateCheck && this.rows.contains(row)) {
                def Integer firstDuplicate = this.rows.findIndexOf { it == row }
                this.errors << "The samplesheet contains duplicate rows for entry ${firstDuplicate + 1} and entry ${getCount()} (${row})".toString()
            }
            this.rows.add(row)

            def Map meta = [:]
            def ArrayList output = []

            for( Map.Entry<String, Map> field : schemaFields ){
                def String key = containsHeader ? field.key : "empty"
                def String input = row[key]

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
                        this.errors << addSample("${dependencies} field(s) should be defined when '${key}' is specified, but the field(s) ${missingValues} is/are not defined.".toString())
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
                        this.errors << addSample("The '${key}' value needs to be unique. '${input}' was found at least twice in the samplesheet.".toString())
                    }
                    booleanUniques[key].add(input)
                }
                else if(unique && uniqueIsList) {
                    def Map<String,String> newMap = (Map) row.subMap((List) [key] + (List) unique)
                    if(!(key in listUniques)){
                        listUniques[key] = []
                    }
                    if(newMap in listUniques[key] && input){
                        this.errors << addSample("The combination of '${key}' with fields ${unique} needs to be unique. ${newMap} was found at least twice.".toString())
                    }
                    listUniques[key].add(newMap)
                }

                // Convert field to a meta field or add it as an input to the channel
                def List<String> metaNames = field['value']['meta'] as List<String>
                if(metaNames) {
                    for(name : metaNames) {
                        meta[name] = (input != '' && input) ? 
                                castToType(input, field) : 
                            field['value']['default'] != null ? 
                                castToType(field['value']['default'] as String, field) : 
                                null
                    }
                }
                else {
                    def inputFile = (input != '' && input) ? 
                            castToType(input, field) : 
                        field['value']['default'] != null ? 
                            castToType(field['value']['default'] as String, field) : 
                            []
                    output.add(inputFile)
                }
            }
            // Add meta to the output when a meta field has been created
            if(meta != [:]) { output.add(0, meta) }
            return output
        }

        // check for samplesheet errors
        if (this.hasErrors()) {
            String message = "Samplesheet errors:\n" + this.getErrors().join("\n")
            throw new SchemaValidationException(message, this.getErrors() as List)
        }

        // check for schema errors
        if (this.hasSchemaErrors()) {
            String message = "Samplesheet schema errors:\n" + this.getSchemaErrors().join("\n")
            throw new SchemaValidationException(message, this.getSchemaErrors() as List)
        }

        // check for warnings
        if( this.hasWarnings() ) {
            def msg = "Samplesheet warnings:\n" + this.getWarnings().join('\n')
            log.warn(msg)
        }

        return outputs
    }

    // Function to infer the file type of the samplesheet
    public static String getFileType(
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
            throw new Exception("Could not derive file type from ${samplesheetFile}. Please specify the file extension (CSV, TSV, YML and YAML are supported).".toString())
        }
        if ( commaCount > tabCount ){
            return "csv"
        }
        else {
            return "tsv"
        }
    }

    // Function to get the header from a CSV or TSV file
    public static String getHeader(
        Path samplesheetFile
    ) {
        def String header
        samplesheetFile.withReader { header = it.readLine() }
        return header
    }

    // Function to transform an input field from the samplesheet to its desired type
    private static castToType(
        String input,
        Map.Entry<String, Map> field
    ) {
        def String type = field['value']['type']
        def String key = field.key

        // Convert string values
        if(type == "string" || !type) {
            def String result = input as String
            
            // Check and convert to the desired format
            def String format = field['value']['format']
            if(format) {
                if(format == "file-path-pattern") {
                    def ArrayList inputFiles = Nextflow.file(input) as ArrayList
                    return inputFiles
                }
                if(format.contains("path")) {
                    def Path inputFile = Nextflow.file(input) as Path
                    return inputFile
                }
            }
            

            // Return the plain string value
            return result
        }

        // Convert number values
        else if(type == "number") {
            try {
                def int result = input as int
                return result
            }
            catch (NumberFormatException e) {
                log.debug("Could not convert ${input} to an integer. Trying to convert to a float.")
            }

            try {
                def float result = input as float
                return result
            }
            catch (NumberFormatException e) {
                log.debug("Could not convert ${input} to a float. Trying to convert to a double.")
            }
            
            def double result = input as double
            return result
        }

        // Convert integer values
        else if(type == "integer") {

            def int result = input as int
            return result
        }

        // Convert boolean values
        else if(type == "boolean") {

            if(input.toLowerCase() == "true") {
                return true
            }
            return false
        }

        else if(type == "null") {
            return null
        }
    }

    private static String addSample (
        String message
    ) {
        return "Entry ${this.getCount()}: ${message}".toString()
    }
}
