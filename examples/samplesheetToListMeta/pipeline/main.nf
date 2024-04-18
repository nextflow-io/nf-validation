include { samplesheetToList } from 'plugin/nf-schema'

ch_input = Channel.fromList(samplesheetToList(params.input, "assets/schema_input.json"))

ch_input.view()
