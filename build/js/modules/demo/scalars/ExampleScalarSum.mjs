export function execute(params, inputs, outputs) {
    // return params.a + params.b
    // - simplest variant to do the same
    outputs.output = parseFloat(params.a) + parseFloat(params.b)
    print(typeof(outputs.output) + ": " + outputs.output)
    return "Hi" // ignored: overridden by outputs.output
}
