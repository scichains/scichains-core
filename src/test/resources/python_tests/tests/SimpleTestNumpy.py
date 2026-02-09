import sys

import numpy
import numpy as np


def demo():
    a = np.array([2, 3, 4])
    b = numpy.array([333])
    return str(a);


if __name__ == '__main__':
    result = demo()
    print(result)
    print(np.array_str(result))
    print(sys.version)
    print(sys.path)
