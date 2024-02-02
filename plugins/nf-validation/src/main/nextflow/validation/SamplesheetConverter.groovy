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
    private static Map meta = [:]

    private static Map getMeta() {
        this.meta
    }

    private static Map resetMeta() {
        this.meta = [:]
    }

    private static addMeta(Map newEntries) {
        this.meta = this.meta + newEntries
    }

    private static Boolean isMeta() {
        this.meta.size() > 0
    }

    public static List convertToList(Path samplesheetFile, Path schemaFile) {

        def LinkedHashMap schemaMap = new JsonSlurper().parseText(schemaFile.text) as LinkedHashMap
        def List samplesheetList = Utils.fileToList(samplesheetFile, schemaFile)

        this.rows = []

        def List channelFormat = samplesheetList.collect { entry ->
            resetMeta()
            def Object result = formatEntry(entry, schemaMap["items"] as LinkedHashMap)
            if (result instanceof List) {
                result.add(0,getMeta())
            } else if (isMeta()) {
                result = [getMeta(), result]
            }
            return result
        }
        return channelFormat
    }

    // TODO add path casting
    private static Object formatEntry(Object input, LinkedHashMap schema) {

        if (input instanceof Map) {
            def List result = []
            def LinkedHashMap properties = schema["properties"]
            properties.each { property, schemaValues ->
                def value = input[property] ?: []
                def List metaIds = schemaValues["meta"] instanceof List ? schemaValues["meta"] as List : schemaValues["meta"] instanceof String ? [schemaValues["meta"]] : []
                if (metaIds) {
                    metaIds.each {
                        addMeta(["${it}":value])
                    }
                } else {
                    result.add(formatEntry(value, schemaValues as LinkedHashMap))
                }
            }
            return result
        } else if (input instanceof List) {
            def List result = []
            input.each {
                result.add(formatEntry(it, schema["items"] as LinkedHashMap))
            }
            return result
        } else {
            def List formats = getPathFormats(schema)
            if (formats && input instanceof String) {
                return Nextflow.file(input)
            }
            return input
        }

    }

    private static List validPathFormats = ["file-path", "path", "directory-path", "file-path-pattern"]

    private static List getPathFormats(Map schemaEntry) {
        def List formats = []
        formats.add(schemaEntry.format ?: [])
        def List options = ["anyOf", "oneOf", "allOf"]
        options.each { option ->
            if(schemaEntry[option]) {
                schemaEntry[option].each {
                    formats.add(it["format"] ?: [])
                }
            }
        }
        return formats.findAll { it != [] && this.validPathFormats.contains(it) }
    }

}
