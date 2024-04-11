include { fromSamplesheet } from 'plugin/nf-schema'

ch_input = Channel.of(params.input).fromSamplesheet("assets/schema_input.json")

ch_input.view()
