def execute(params, inputs, outputs):
    outputs.output = params.a + params.b
    return "Hi" # ignored: overridden by outputs.output
