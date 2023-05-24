import numpy as np
import cv2 as cv


def blurMat(src, ksize):
    ksize |= 0x1;
    return cv.GaussianBlur(src, (ksize, ksize), cv.BORDER_DEFAULT)


def execute(params, inputs, outputs):
    result = "Hello from ExampleMatBlur"
    if params.p <= 0:
        raise Exception("Parameter \"p\" must contain correct odd kernel size")
    outputs.a = np.average(inputs.m1)
    outputs.m1 = blurMat(inputs.m1, round(params.p))
    if params.q > 0:
        cv.imshow("Gaussian blur", np.hstack((inputs.m1, outputs.m1)))
        cv.waitKey(0)
        cv.destroyAllWindows()
    return result
