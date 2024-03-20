include { fromSamplesheet } from 'plugin/nf-schema'

ch_input = Channel.fromSamplesheet("input")

ch_input.view()
