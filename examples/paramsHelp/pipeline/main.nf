include { paramsHelp } from 'plugin/nf-schema'

if (params.help) {
    log.info paramsHelp("nextflow run my_pipeline --input input_file.csv")
    exit 0
}
