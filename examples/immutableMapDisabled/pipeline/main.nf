include { fromSamplesheet } from 'plugin/nf-validation'

// This will work
ch_input = Channel.fromSamplesheet("input", immutable_meta:false)
ch_input
    .map { 
        it[0].put("new_key", "test")
        return it
    }
    .view()