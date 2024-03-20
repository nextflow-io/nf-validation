package nextflow.validation

import org.yaml.snakeyaml.Yaml
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONPointer
import org.json.JSONPointerException
import nextflow.Global

import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import java.nio.file.Path

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
public class Utils {

    // Function to infer the file type of a samplesheet
    public static String getFileType(Path file) {
        def String extension = file.getExtension()
        if (extension in ["csv", "tsv", "yml", "yaml", "json"]) {
            return extension == "yml" ? "yaml" : extension
        }

        def String header = getHeader(file)

        def Integer commaCount = header.count(",")
        def Integer tabCount = header.count("\t")

        if ( commaCount == tabCount ){
            log.error("Could not derive file type from ${file}. Please specify the file extension (CSV, TSV, YML, YAML and JSON are supported).".toString())
        }
        if ( commaCount > tabCount ){
            return "csv"
        }
        else {
            return "tsv"
        }
    }

    // Function to get the header from a CSV or TSV file
    public static String getHeader(Path file) {
        def String header
        file.withReader { header = it.readLine() }
        return header
    }

    // Converts a given file to a List
    public static List fileToList(Path file, Path schema) {
        def String fileType = Utils.getFileType(file)
        def String delimiter = fileType == "csv" ? "," : fileType == "tsv" ? "\t" : null
        def Map types = variableTypes(schema)

        if (types.find{ it.value == "array" } as Boolean && fileType in ["csv", "tsv"]){
            def msg = "Using \"type\": \"array\" in schema with a \".$fileType\" samplesheet is not supported\n"
            log.error("ERROR: Validation of pipeline parameters failed!")
            throw new SchemaValidationException(msg, [])
        }

        if(fileType == "yaml"){
            return new Yaml().load((file.text))
        }
        else if(fileType == "json"){
            return new JsonSlurper().parseText(file.text) as List
        }
        else {
            def Boolean header = getValueFromJson("#/items/properties", new JSONObject(schema.text)) ? true : false
            def List fileContent = file.splitCsv(header:header, strip:true, sep:delimiter, quote:'\"')
            if (!header) {
                // Flatten no header inputs if they contain one value
                fileContent = fileContent.collect { it instanceof List && it.size() == 1 ? it[0] : it }
            }

            return castToType(fileContent)
        }
    }

    // Converts a given file to a JSONArray
    public static JSONArray fileToJsonArray(Path file, Path schema) {
        // Remove all null values from JSON object
        // and convert the groovy object to a JSONArray
        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()
        return new JSONArray(jsonGenerator.toJson(fileToList(file, schema)))
    }

    //
    // Cast a value to the provided type in a Strict mode
    //

    public static Object castToType(Object input) {
        def Set<String> validBooleanValues = ['true', 'false'] as Set

        if (input instanceof Map) {
            // Cast all values in the map
            def Map output = [:]
            input.each { k, v ->
                output[k] = castToType(v)
            }
            return output
        }
        else if (input instanceof List) {
            // Cast all values in the list
            def List output = []
            for( entry : input ) {
                output.add(castToType(entry))
            }
            return output
        } else if (input instanceof String) {
            // Cast the string if there is one
            if (input == "") {
                return null
            }
            return JSONObject.stringToValue(input)
        }
    }

    // Resolve Schema path relative to main workflow directory
    public static String getSchemaPath(String baseDir, String schemaFilename='nextflow_schema.json') {
        if (Path.of(schemaFilename).exists()) {
            return schemaFilename
        } else {
            return "${baseDir}/${schemaFilename}"
        }
    }

    // Function to obtain the variable types of properties from a JSON Schema
    public static Map variableTypes(Path schema) {
        def Map variableTypes = [:]
        def String type = ''

        // Read the schema
        def slurper = new JsonSlurper()
        def Map parsed = (Map) slurper.parse( schema )

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
                variableTypes[key] = 'string' // If there isn't a type specified, return 'string' to avoid having a null value
            }
        }

        return variableTypes
    }

    // Function to check if a String value is an Integer
    public static Boolean isInteger(String input) {
        try {
            input as Integer
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    // Function to check if a String value is a Float
    public static Boolean isFloat(String input) {
        try {
            input as Float
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    // Function to check if a String value is a Double
    public static Boolean isDouble(String input) {
        try {
            input as Double
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    // Function to get the value from a JSON pointer
    public static Object getValueFromJson(String jsonPointer, Object json) {
        def JSONPointer schemaPointer = new JSONPointer(jsonPointer)
        try {
            return schemaPointer.queryFrom(json) ?: ""
        } catch (JSONPointerException e) {
            return ""
        }
    }
}