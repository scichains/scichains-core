from net.algart.executors.modules.core.matrices.conversions import Intensity
from net.algart.executors.api.jep import Jep2SMat

def execute(params, inputs, outputs):
    intensity = Intensity()
    intensity.setRgbResult(params.rgbResult)
    mat = Jep2SMat.toSMat(inputs.input)
    result = intensity.process(mat);
    print("~~~~Python example intensity result: " + str(result))
    return Jep2SMat.toNDArray(result)
