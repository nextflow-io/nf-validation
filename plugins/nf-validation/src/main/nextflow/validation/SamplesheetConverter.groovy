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
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

import nextflow.Channel
import nextflow.Global
import nextflow.Nextflow
import nextflow.plugin.extension.Function
import nextflow.Session

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : awgymer
 */

@Slf4j
@CompileStatic
class SamplesheetConverter {

    private static List<Map> rows = []

    static List convertToList(Path samplesheetFile, Path schemaFile, Boolean header) {

        def JSONObject schemaMap = new JSONObject(schemaFile.text)
        def JSONArray samplesheetList = Utils.fileToJsonArray(samplesheetFile, schemaFile)

        this.rows = []

        def Iterator<Object> samplesheetIterator = samplesheetList.iterator()

        while (samplesheetIterator.hasNext()) {
            println(samplesheetIterator.next())
        }

        // def List outputs = samplesheetList.collect { fullRow ->

        //     println(fullrow.getClass())

            // def Map<String,Object> row = fullRow.findAll { it.value != "" }
            // def Set rowKeys = header ? row.keySet() : ["empty"].toSet()

            // Check the header (CSV/TSV) or present fields (YAML)
            // TODO reimplement warning for unused fields

            // this.rows.add(row)

            // def Map meta = [:]
            // def ArrayList output = []

            // for( Map.Entry<String, Map> field : schemaFields ){
            //     def String key = header ? field.key : "empty"
            //     def Object input = row[key]

            //     // Convert field to a meta field or add it as an input to the channel
            //     def List<String> metaNames = field['value']['meta'] as List<String>
            //     if(metaNames) {
            //         for(name : metaNames) {
            //             meta[name] = (input != '' && input != null) ?
            //                     castToNFType(input, field) :
            //                 field['value']['default'] != null ?
            //                     castToNFType(field['value']['default'], field) :
            //                     null
            //         }
            //     }
            //     else {
            //         def inputVal = (input != '' && input != null) ?
            //                 castToNFType(input, field) :
            //             field['value']['default'] != null ?
            //                 castToNFType(field['value']['default'], field) :
            //                 []
            //         output.add(inputVal)
            //     }
            // }
            // // Add meta to the output when a meta field has been created
            // if(meta != [:]) { output.add(0, meta) }
        //     return []
        // }

        return []
    }

    // Function to transform an input field from the samplesheet to its desired type
    private static castToNFType(
        Object input,
        Map.Entry<String, Map> field
    ) {
        def String type = field['value']['type']
        def String key = field.key

        // Recursively call this function for each item in the array if the field is an array-type
        // The returned values are collected into a single array
        if (type == "array") {
            def Map.Entry<String, Map> subfield = (Map.Entry<String, Map>) Map.entry(field.key, field['value']['items'])
            log.debug "subfield = $subfield"
            def ArrayList result = input.collect{ castToNFType(it, subfield) } as ArrayList
            return result
        }

        def String inputStr = input as String
        // Convert string values
        if(type == "string" || !type) {
            def String result = inputStr as String
            
            // Check and convert to the desired format
            def String format = field['value']['format']
            if(format) {
                if(format == "file-path-pattern") {
                    def ArrayList inputFiles = Nextflow.file(inputStr) as ArrayList
                    return inputFiles
                }
                if(format.contains("path")) {
                    def Path inputFile = Nextflow.file(inputStr) as Path
                    return inputFile
                }
            }
            

            // Return the plain string value
            return result
        }

        // Convert number values
        else if(type == "number") {
            try {
                def int result = inputStr as int
                return result
            }
            catch (NumberFormatException e) {
                log.debug("Could not convert ${input} to an integer. Trying to convert to a float.")
            }

            try {
                def float result = inputStr as float
                return result
            }
            catch (NumberFormatException e) {
                log.debug("Could not convert ${inputStr} to a float. Trying to convert to a double.")
            }
            
            def double result = inputStr as double
            return result
        }

        // Convert integer values
        else if(type == "integer") {

            def int result = inputStr as int
            return result
        }

        // Convert boolean values
        else if(type == "boolean") {

            if(inputStr.toLowerCase() == "true") {
                return true
            }
            return false
        }

        else if(type == "null") {
            return null
        }
    }

}
