import sys
import numpy as np
import array
import tracemalloc
import time

N = 1024 * 2048

class SimpleOperationSpeed:

    def testList(self):
        t1 = time.perf_counter_ns()
        t2 = time.perf_counter_ns()
        printSimpleTime("time.perf_counter_ns", t1, t2)

        t1 = time.perf_counter_ns()
        a = [0] * N
        t2 = time.perf_counter_ns()
        printTime("Allocation (0)", t1, t2)

        t1 = time.perf_counter_ns()
        a = [i for i in range(N)]
        t2 = time.perf_counter_ns()
        printTime("Allocation and filling (0,1,2,...)", t1, t2)


    def testArray(self):
        t1 = time.perf_counter_ns()
        a = array.array('i', (0 for i in range(0, N)))
        t2 = time.perf_counter_ns()
        printTime("Allocation (0)", t1, t2)

        t1 = time.perf_counter_ns()
        a = array.array('i', (i for i in range(0, N)))
        t2 = time.perf_counter_ns()
        printTime("Allocation and filling (0,1,2,...)", t1, t2)

        t1 = time.perf_counter_ns()
        for i in range(0, N):
            a[i] += 1
        t2 = time.perf_counter_ns()
        printTime("Incrementing by 1", t1, t2)


    def testNdArray(self):
        t1 = time.perf_counter_ns()
        a = np.zeros(N, dtype=np.int32)
        t2 = time.perf_counter_ns()
        printTime("Allocation (0)", t1, t2)

        t1 = time.perf_counter_ns()
        a = np.arange(0, N, 1, np.int32)
        t2 = time.perf_counter_ns()
        printTime("Allocation and filling (0,1,2,...)", t1, t2)

        t1 = time.perf_counter_ns()
        a += 1
        t2 = time.perf_counter_ns()
        printTime("Incrementing by 1", t1, t2)

    def testAll(self):
        for test in range(5):
            print("\nTest list #%d" % test)
            self.testList()
            print("Test array #%d" % test)
            self.testArray()
            print("Test numpy array #%d" % test)
            self.testNdArray()
        return sys.path

def testNoClass():
    return SimpleOperationSpeed().testAll()

def printMemory():
    mem = tracemalloc.get_traced_memory()[0]
    print("Current: %f, %f per element" % (mem, mem / N))


def printSimpleTime(name, t1, t2):
    print("%-36s %.6f ns" % (name + ":", t2 - t1))


def printTime(name, t1, t2):
    print("%-36s %.9f ms, %.6f ns/element" % (name + ":", (t2 - t1) * 1e-6, (t2 - t1) / N))


if __name__ == '__main__':
    testNoClass()
