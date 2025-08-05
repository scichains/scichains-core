import numpy

def execute(params, inputs, outputs):
    result = "Hello from Python demo!"
    outputs.a = params.p + params.q
    outputs.b = numpy.sum(inputs.x1)
    outputs.x1 = inputs.x1
    outputs.m1 = inputs.m1
    return result
