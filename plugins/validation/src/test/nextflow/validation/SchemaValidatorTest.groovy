package nextflow.validation

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class SchemaValidatorTest extends Specification {

    def 'should validate a schema' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [transcriptome: '/some/path']
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        !validator.hasErrors()
        !validator.hasWarnings()

    }

    def 'should found unexpected params' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [xyz: '/some/path']
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        validator.hasWarnings()
        validator.warnings == ['* --xyz: /some/path']
        and:
        !validator.hasErrors()
    }

    def 'should find validation errors' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [outdir: 10]
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        validator.hasErrors()
        validator.errors == [ '* --outdir: expected type: String, found: Integer (10)' ]
        and:
        !validator.hasWarnings()
    }

    def 'should correctly validate duration and memory objects' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [max_memory: 10.GB, max_time: 10.d]
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        !validator.hasErrors()
        !validator.hasWarnings()
    }

    def 'should find validation errors for enum' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [publish_dir_mode: "incorrect"]
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        validator.hasErrors()
        validator.errors == [ "* --publish_dir_mode: 'incorrect' is not a valid choice (Available choices (5 of 6): symlink, rellink, link, copy, copyNoFollow, ... )" ]
    }

    def 'correct validation of integers' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [max_cpus: 12]
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        !validator.hasErrors()
        !validator.hasWarnings()
    }

    def 'correct validation of numbers' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [generic_number: 0.43]
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        !validator.hasWarnings()
        !validator.hasErrors()
    }

    def 'should fail because of incorrect integer' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [max_cpus: 1.2]
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        validator.hasErrors()
        validator.errors == [ '* --max_cpus: expected type: Integer, found: BigDecimal (1.2)' ]
        !validator.hasWarnings()
    }

    def 'should fail because of wrong pattern' () {
        given:
        def validator = new SchemaValidator()

        when:
        def params = [max_memory: '10']
        validator.validateParameters(params, '/some/path', SCHEMA)

        then:
        validator.hasErrors()
        validator.errors == [ '* --max_memory: string [10] does not match pattern ^[\\d\\.]+\\s*.(K|M|G|T)?B$ (10)' ]
        !validator.hasWarnings()
    }

    static String SCHEMA = '''
            {
              "$schema": "http://json-schema.org/draft-07/schema",
              "$id": "https://raw.githubusercontent.com/master/nextflow_schema.json",
              "title": "Rnaseq-nf pipeline pipeline parameters",
              "description": "Proof of concept of a RNA-seq pipeline implemented with Nextflow",
              "type": "object",
              "definitions": {
                "input_output_options": {
                  "title": "Input/output options",
                  "type": "object",
                  "fa_icon": "fas fa-terminal",
                  "description": "Define where the pipeline should find input data and save output data.",
                  "properties": {
                    "transcriptome": {
                      "type": "string",
                      "description": "The input transcriptome file",
                      "default": "s3://rnaseq-nf/data/ggal/transcript.fa",
                      "fa_icon": "fas fa-folder-open"
                    },
                    "reads": {
                      "type": "string",
                      "description": "The input read-pair files",
                      "default": "s3://rnaseq-nf/data/ggal/lung_{1,2}.fq",
                      "fa_icon": "fas fa-folder-open"
                    },
                    "outdir": {
                      "type": "string",
                      "description": "The output directory where the results will be saved.",
                      "default": "./results",
                      "fa_icon": "fas fa-folder-open"
                    }
                  }
                },
                "reference_genome_options": {
                  "title": "Reference genome options",
                  "type": "object",
                  "fa_icon": "fas fa-dna",
                  "description": "Options for the reference genome indices used to align reads.",
                  "properties": {}
                },
                "generic_options": {
                  "title": "Generic options",
                  "type": "object",
                  "fa_icon": "fas fa-file-import",
                  "description": "Less common options for the pipeline, typically set in a config file.",
                  "help_text": "These options are common to all nf-core pipelines and allow you to customise some of the core preferences for how the pipeline runs.\\n\\nTypically these options would be set in a Nextflow config file loaded for all pipeline runs, such as `~/.nextflow/config`.",
                  "properties": {
                    "publish_dir_mode": {
                    "type": "string",
                    "default": "copy",
                    "description": "Method used to save pipeline results to output directory.",
                    "help_text": "The Nextflow `publishDir` option specifies which intermediate files should be saved to the output directory. This option tells the pipeline what method should be used to move these files. See [Nextflow docs](https://www.nextflow.io/docs/latest/process.html#publishdir) for details.",
                    "fa_icon": "fas fa-copy",
                    "enum": [
                        "symlink",
                        "rellink",
                        "link",
                        "copy",
                        "copyNoFollow",
                        "move"
                    ]
                    },
                    "generic_number": {
                    "type": "number",
                    "default": 0.5,
                    "description": "A random number for testing purposes"
                    }
                  }
                },
                "max_job_request_options": {
                  "title": "Max job request options",
                  "type": "object",
                  "fa_icon": "fab fa-acquisitions-incorporated",
                  "description": "Set the top limit for requested resources for any single job.",
                  "help_text": "If you are running on a smaller system, a pipeline step requesting more resources than are available may cause the Nextflow to stop the run with an error. These options allow you to cap the maximum resources requested by any single job so that the pipeline will run on your system.\\n\\nNote that you can not _increase_ the resources requested by any job using these options. For that you will need your own configuration file. See [the nf-core website](https://nf-co.re/usage/configuration) for details.",
                  "properties": {
                "max_cpus": {
                    "type": "integer",
                    "description": "Maximum number of CPUs that can be requested for any single job.",
                    "default": 16,
                    "fa_icon": "fas fa-microchip",
                    "hidden": true,
                    "help_text": "Use to set an upper-limit for the CPU requirement for each process. Should be an integer e.g. `--max_cpus 1`"
                },
                "max_memory": {
                    "type": "string",
                    "description": "Maximum amount of memory that can be requested for any single job.",
                    "default": "128.GB",
                    "fa_icon": "fas fa-memory",
                    "pattern": "^[\\\\d\\\\.]+\\\\s*.(K|M|G|T)?B$",
                    "hidden": true,
                    "help_text": "Use to set an upper-limit for the memory requirement for each process. Should be a string in the format integer-unit e.g. `--max_memory '8.GB'`"
                },
                "max_time": {
                    "type": "string",
                    "description": "Maximum amount of time that can be requested for any single job.",
                    "default": "240.h",
                    "fa_icon": "far fa-clock",
                    "hidden": true,
                    "pattern": "^[\\\\d\\\\.]+\\\\.*(s|m|h|d)$",
                    "help_text": "Use to set an upper-limit for the time requirement for each process. Should be a string in the format integer-unit e.g. `--max_time '2.h'`"
                }
               }   
                },
                "institutional_config_options": {
                  "title": "Institutional config options",
                  "type": "object",
                  "fa_icon": "fas fa-university",
                  "description": "Parameters used to describe centralised config profiles. These should not be edited.",
                  "help_text": "The centralised nf-core configuration profiles use a handful of pipeline parameters to describe themselves. This information is then printed to the Nextflow log when you run a pipeline. You should not need to change these values when you run a pipeline.",
                  "properties": {}
                }
              },
              "allOf": [
                {
                  "$ref": "#/definitions/input_output_options"
                },
                {
                  "$ref": "#/definitions/reference_genome_options"
                },
                {
                  "$ref": "#/definitions/generic_options"
                },
                {
                  "$ref": "#/definitions/max_job_request_options"
                },
                {
                  "$ref": "#/definitions/institutional_config_options"
                }
              ],
              "properties": {
                "reads": {
                  "type": "string"
                },
                "transcriptome": {
                  "type": "string"
                },
                "multiqc": {
                  "type": "string"
                }
              }
            }
            '''


}
