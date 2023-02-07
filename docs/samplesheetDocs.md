# Samplesheet JSON schema

This page will describe the custom function that are implemented in this plugin for samplesheet validation and conversion. All functionality from JSON schema draft 4-7 is supported. For these functions, see the [official JSON schema docs](https://json-schema.org/).

Example samplesheets can be found in the [references](references/) folder.

## Properties
All fields should be present in the `properties` section. These should be in the order you want for the output channel e.g. for this [schema](references/samplesheet_schema.json) the output channel will look like `[[id:sample, sample:sample], cram, crai, bed, ped]`.

### Global parameters
These schema specifications can be used on any type of input:
<table>
    <tr style="font-weight: bold">
        <td> Parameter </td><td> Type </td><td> Description </td><td> Example </td>
    </tr>
    <tr>
        <td> meta </td>
        <td> List </td>
        <td> The current field will be considered a meta value when this parameter is present. This parameter should contain a list of the meta fields to assign this value to. The default is no meta for each field. </td>
        <td> 

```json
"field": {
    "meta": ["id","sample"]
}
``` 
will convert the `field` value to a meta value, resulting in the channel `[[id:value, sample:value]...]` 
        </td>
    </tr>
    <tr>
        <td> unique </td>
        <td> Boolean or List </td>
        <td> Whether or not the field should contain a unique value over the entire samplesheet. A list can also be given to this parameter. This list should contain other field names that should be unique in combination with the current field. The default is `false`. </td>
        <td>

```json
"field1": {
    "unique":true
},
"field2": {
    "unique": ["field1"]
}
```
`field1` needs to be unique in this example. `field2` needs to be unique in combination with `field1`. So for a samplesheet like this:
```csv
field1,field2
value1,value2
value1,value3
value1,value2
```
both checks will fail. `field1` isn't unique since `value1` has been found more than once. `field2` isn't unique in combination with `field1` because the `value1,value2` combination has been found more than once.
        </td>
    </tr>
    <tr>
        <td> deprecated </td>
        <td> Boolean </td>
        <td> A boolean variable stating that the field is deprecated and will be removed in the nearby future. This will throw a warning to the user that the current field is deprecated. The default value is `false` </td>
        <td>
```json
"field": {
    "deprecated": true
}
```
will show a warning stating that the use of `field` is deprecated: `The 'field' field is deprecated and will no longer be used in the future. Please check the official documentation of the pipeline for more information.`
        </td>
    </tr>
    <tr>
        <td> dependentRequired </td>
        <td> List </td>
        <td> A list containing names of other fields. The validator will check if these fields are filled in and throw an error if they aren't, but only when the field `dependentRequired` belongs to is filled in. </td>
        <td>

```json
"field1": {
    "dependentRequired": ["field2"]
},
"field2": {}
```
will check if `field2` is given when `field1` has a value. So for example:
```csv
field1,field2
value1,value2
value1,
,value2
```
The first row will pass the check because both fields are set. The second row will fail because `field1` is set, but `field2` isn't and `field1` is dependent on `field2`. The third row will pass the check because `field1` isn't set.
        </td>
    </tr>
</table>

### Formats

Formats can be used to check `string` values for certain properties.

Following table shows all additional formats implemented in this plugin. These formats will also be converted to the correct type.

| Format | Description |
|-----------|-------------|
| file-path | Automatically checks if the file exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input |
| directory-path | Automatically checks if the directory exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input. This is currently synonymous for `file-path`. |

You can use the formats like this:
```json
"field": {
    "type":"string",
    "format":"file-path"
}
```

