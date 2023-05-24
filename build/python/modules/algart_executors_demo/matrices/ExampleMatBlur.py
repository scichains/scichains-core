import cv2 as cv

def blurMat(src, ksize):
    ksize |= 0x1
    return cv.GaussianBlur(src, (ksize, ksize), 0, borderType=cv.BORDER_DEFAULT)


def execute(params, inputs, outputs):
    return blurMat(inputs.input, params.kernelSize)
