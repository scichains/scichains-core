import numpy as np


def execute(params, inputs, outputs):
    result = "Hello from ExampleArray"
    print(result)
    outputs.a = params.p + params.q
    outputs.b = np.sum(inputs.x1)
    outputs.x1 = inputs.x1
    outputs.m1 = inputs.m1
    outputs.x2 = np.array([[1, 1, 1, 1, 1],
                           [2, 2, 2, 2, 2],
                           [3, 3, 3, 3, 3]])
    return result
