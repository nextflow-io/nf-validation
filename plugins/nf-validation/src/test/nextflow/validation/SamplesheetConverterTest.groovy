package nextflow.validation

import java.nio.file.Path

import nextflow.plugin.Plugins
import nextflow.plugin.TestPluginDescriptorFinder
import nextflow.plugin.TestPluginManager
import nextflow.plugin.extension.PluginExtensionProvider
import org.junit.Rule
import org.pf4j.PluginDescriptorFinder
import spock.lang.Shared
import test.Dsl2Spec
import test.OutputCapture
/**
 * @author : Nicolas Vannieuwkerke <nicolas.vannieuwkerke@ugent.be>
 *
 */
class SamplesheetConverterTest extends Dsl2Spec{

    @Rule
    OutputCapture capture = new OutputCapture()


    @Shared String pluginsMode

    Path root = Path.of('.').toAbsolutePath().normalize()
    Path getRoot() { this.root }
    String getRootString() { this.root.toString() }

    def setup() {
        // reset previous instances
        PluginExtensionProvider.reset()
        // this need to be set *before* the plugin manager class is created
        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')
        // the plugin root should
        def root = this.getRoot()
        def manager = new TestPluginManager(root){
            @Override
            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return new TestPluginDescriptorFinder(){
                    @Override
                    protected Path getManifestPath(Path pluginPath) {
                        return pluginPath.resolve('build/resources/main/META-INF/MANIFEST.MF')
                    }
                }
            }
        }
        Plugins.init(root, 'dev', manager)
    }

    def cleanup() {
        Plugins.stop()
        PluginExtensionProvider.reset()
        pluginsMode ? System.setProperty('pf4j.mode',pluginsMode) : System.clearProperty('pf4j.mode')
    }

    def 'should work fine - CSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/correct.csv'

            workflow {
                Channel.fromSamplesheet("input", "src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:5, integer2:5, boolean1:true, boolean2:true], string1, 25, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

        def 'should work fine - TSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/correct.csv'

            workflow {
                Channel.fromSamplesheet("input", "src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:5, integer2:5, boolean1:true, boolean2:true], string1, 25, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - YAML' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/correct.csv'

            workflow {
                Channel.fromSamplesheet("input", "src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:5, integer2:5, boolean1:true, boolean2:true], string1, 25, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'extra field' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/extraFields.csv'

            workflow {
                Channel.fromSamplesheet("input", "src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') || it.contains('The samplesheet contains following unchecked field(s): [extraField]') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("\tThe samplesheet contains following unchecked field(s): [extraField]")
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, [], unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:5, integer2:5, boolean1:true, boolean2:true], string1, 25, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, [], unique3, 1, itDoesExist]" as String)
    }

    def 'no meta' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/no_meta.csv'

            workflow {
                Channel.fromSamplesheet("input", "src/testResources/nextflow_schema_with_samplesheet_no_meta.json").view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[test1, test2]")
    }

    def 'errors' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/errors.csv'

            workflow {
                Channel.fromSamplesheet("input", "src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines() 
        errorMessages[0] == "Samplesheet errors:"
        errorMessages[1] == "\tEntry 1: [metaInteger, metaBoolean] field(s) should be defined when 'metaString' is specified, but the field(s) [metaInteger] is/are not defined."
        errorMessages[2] == "\tEntry 3: The 'uniqueField' value needs to be unique. 'non_unique' was found at least twice in the samplesheet."
        errorMessages[3] == "\tEntry 3: The combination of 'uniqueDependentField' with fields [uniqueField] needs to be unique. [uniqueDependentField:1, uniqueField:non_unique] was found at least twice."
        !stdout
    }

        def 'errors before channel conversion' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/errorsBeforeConversion.csv'

            workflow {
                Channel.fromSamplesheet("input", "src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines() 
        errorMessages[0] == "\033[0;31mThe following errors have been detected:"
        errorMessages[2] == "* -- Entry 1 - number: expected type: Number, found: String (string)"
        errorMessages[3] == "* -- Entry 1 - path: the file or directory 'non_existing_path' does not exist (non_existing_path)"
        errorMessages[4] == "* -- Entry 1 - boolean: expected type: Boolean, found: String (20)"
        errorMessages[5] == '* -- Entry 1 - file: string [non_existing_file.tsv] does not match pattern ^.*\\.txt$ (non_existing_file.tsv)'
        errorMessages[6] == "* -- Entry 1 - file: the file 'non_existing_file.tsv' does not exist (non_existing_file.tsv)"
        errorMessages[7] == "* -- Entry 1 - directory: 'src/testResources/test.txt' is not a directory (src/testResources/test.txt)"
        errorMessages[8] == "* -- Entry 2: Missing required value: string"
        errorMessages[9] == "* -- Entry 2: Missing required value: number"
        errorMessages[10] == "* -- Entry 2: Missing required value: boolean"
        errorMessages[11] == "* -- Entry 3 - metaBoolean: expected type: Boolean, found: String (3333)"
        errorMessages[12] == "* -- Entry 3 - metaInteger: expected type: Integer, found: String (false)"
        !stdout
    }
}
