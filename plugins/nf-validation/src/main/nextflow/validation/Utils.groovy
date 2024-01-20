package nextflow.validation

import org.yaml.snakeyaml.Yaml
import groovy.json.JsonSlurper

import groovy.util.logging.Slf4j
import java.nio.file.Path

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

    // Converts a given file to a map
    public static List<Map> fileToMaps(Path file, String schemaName, String baseDir) {
        def String fileType = Utils.getFileType(file)
        def String delimiter = fileType == "csv" ? "," : fileType == "tsv" ? "\t" : null
        def List<Map<String,String>> fileContent
        def Map types = variableTypes(schemaName, baseDir)
        def Boolean containsHeader = !(types.keySet().size() == 1 && types.keySet()[0] == "")

        if(!containsHeader){
            types = ["empty": types[""]]
        }
        if(fileType == "yaml"){
            fileContent = new Yaml().load((file.text)).collect {
                if(containsHeader) {
                    return it as Map
                }
                return ["empty": it] as Map
            }
        }
        else if(fileType == "json"){
            fileContent = new JsonSlurper().parseText(file.text).collect {
                if(containsHeader) {
                    return it as Map
                }
                return ["empty": it] as Map
            }
        }
        else {
            fileContent = file.splitCsv(header:containsHeader ?: ["empty"], strip:true, sep:delimiter, quote:'\"')
        }
        def List<Map<String,String>> fileContentCasted = castToType(fileContent, types)
        return fileContentCasted
    }

    //
    // Cast a value to the provided type in a Strict mode
    //

    public static List castToType(List<Map> rows, Map types) {
        def List<Map> casted = []
        def Set<String> validBooleanValues = ['true', 'false'] as Set

        for( Map row in rows) {
            def Map castedRow = [:]

            for (String key in row.keySet()) {
                def String str = row[key]
                def String type = types[key]

                try {
                    if( str == null || str == '' ) castedRow[key] = null
                    else if( type == null ) castedRow[key] = str
                    else if( type.toLowerCase() == 'boolean' && str.toLowerCase() in validBooleanValues ) castedRow[key] = str.toBoolean()
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

    // Resolve Schema path relative to main workflow directory
    public static String getSchemaPath(String baseDir, String schemaFilename='nextflow_schema.json') {
        if (Path.of(schemaFilename).exists()) {
            return schemaFilename
        } else {
            return "${baseDir}/${schemaFilename}"
        }
    }

    // Function to obtain the variable types of properties from a JSON Schema
    public static Map variableTypes(String schemaFilename, String baseDir) {
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

    public static Boolean isInteger(String input) {
        try {
            input as Integer
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }
}