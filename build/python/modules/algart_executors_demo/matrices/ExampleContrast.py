import numpy as np

def execute(params, inputs, outputs):
    mat = inputs.input.astype('float32')
    min = mat.min()
    max = mat.max()
    mult = 1.0 / (max - min)
    result = (mat - min) * mult
    print(result.dtype)
    return result
