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
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_converter.json").view().first().map {println(it[0].getClass())}
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
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - quoted CSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/correct_quoted.csv'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_converter.json").view().first().map {println(it[0].getClass())}
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
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - TSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/correct.csv'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
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
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - YAML' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/correct.csv'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
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
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - JSON' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/correct.json'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
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
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, ${this.getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'no header - CSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/no_header.csv'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_no_header.json").view()
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
        stdout.contains("[test_1]")
        stdout.contains("[test_2]")
    }

    def 'no header - YAML' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/no_header.yaml'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_no_header.json").view()
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
        stdout.contains("[test_1]")
        stdout.contains("[test_2]")
    }

    def 'no header - JSON' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/no_header.json'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_no_header.json").view()
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
        stdout.contains("[test_1]")
        stdout.contains("[test_2]")
    }


    def 'extra field' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/extraFields.csv'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_converter.json").view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()

        then:
        noExceptionThrown()
        stdout.contains("\tThe samplesheet contains following unchecked field(s): [extraField]")
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, [], unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${this.getRootString()}/src/testResources/test.txt, ${this.getRootString()}/src/testResources/testDir, [], unique3, 1, itDoesExist]" as String)
    }

    def 'no meta' () {
        given:
        def SCRIPT_TEXT = '''
            include { fromSamplesheet } from 'plugin/nf-validation'

            params.input = 'src/testResources/no_meta.csv'

            workflow {
                Channel.fromSamplesheet("input", parameters_schema:"src/testResources/nextflow_schema_with_samplesheet_no_meta.json").view()
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
}
