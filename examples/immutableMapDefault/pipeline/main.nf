include { fromSamplesheet } from 'plugin/nf-validation'

// This will fail
ch_input = Channel.fromSamplesheet("input")
ch_input
    .map { 
        it[0].put("new_key", "test")
        return it
    }
    .view()