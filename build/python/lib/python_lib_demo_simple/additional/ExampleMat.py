import numpy as np


def createTestMat():
    img = np.zeros([1000, 400, 3], dtype=np.uint8)
    img[100:200, 100:200, 0] = 64
    img[150:, 300:, 1] = 128
    img[:, :, 2] = 192
    return img


def execute(params, inputs, outputs):
    result = "Hello from ExampleMat"
    outputs.a = np.average(inputs.m1)
    outputs.m1 = inputs.m1 * 2
    outputs.m2 = createTestMat()
    return result
