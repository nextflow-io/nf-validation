include { fromSamplesheet } from 'plugin/nf-validation'

ch_input = Channel.fromSamplesheet("input")

println ch_input
