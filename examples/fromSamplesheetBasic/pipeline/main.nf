include { fromSamplesheet } from 'plugin/nf-validation'

ch_input = Channel.fromSamplesheet("input")

ch_input.view()
