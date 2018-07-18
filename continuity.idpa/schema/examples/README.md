# YAML Schema Examples for the IDPA
We provide an IDPA application and annotation model for illustrating the use of the provided YAML schemata. Currently, the schemata lack in proper support by YAML validators, but you can always use the Java implementation to check for proper YAML files.

## Example Files
The two example models are:
* application-heat-clinic.yml
* annotation-heat-clinic.yml

The models are to be used with the [Broadleaf Heat Clinic Demo](https://github.com/BroadleafCommerce/DemoSite).

*Please note that the YAML files are slightly changed compared to the normal IDPA format*. Because current JSON/YAML schemata do not properly support YAML tags (such as !<http>), we had to use a workaround which does not work with common YAML validators.

## Validate the Examples
For validating the example YAML files using the schemata, you have to use a YAML validator, e.g., [Polyglottal JSON Schema Validator](https://www.npmjs.com/package/pajv).

Simply go to this folder and run
```
pajv -s ../annotation-schema.yal -d annotation-heat-clinic.yml
```

## Validate other IDPAs
For validating other IDPAs, they first have to be changed similarly to the examples. This can be done by using regular expressions:

### Ensure Properties in Newlines after IDs
* RegEx:       '[\s]{2}-\s(&[\w_]+)\s(\w+:)'
* Replacement: '  - $1\n    $2'

### Correct Type Tag
* RegEx:       '!<(\w+)>\n\s+(&[\w_]+)\s(\w)'
* Replacement: '$2\n  "@type": $1\n  $3'