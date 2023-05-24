import math
import numpy as np

def execute(params, inputs, outputs):
    m1 = inputs.m1
    m2 = inputs.m2
    m3 = inputs.m3
    m4 = inputs.m4
    m5 = inputs.m5
    m6 = inputs.m6
    m7 = inputs.m7
    m8 = inputs.m8
    m9 = inputs.m9
    m10 = inputs.m10
    a = params.a
    b = params.b
    c = params.c
    d = params.d
    e = params.e
    f = params.f
    r = eval(params.formula)
    return r.astype(m1.dtype)

