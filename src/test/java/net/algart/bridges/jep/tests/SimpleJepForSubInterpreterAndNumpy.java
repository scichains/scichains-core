package net.algart.bridges.jep.tests;

import jep.Interpreter;
import jep.SharedInterpreter;
import jep.SubInterpreter;

public class SimpleJepForSubInterpreterAndNumpy {
    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(() -> {
            try (Interpreter interp = new SubInterpreter()) {
                Object result = null;
                System.out.printf("%nInterpreter: %s%n", interp);
// Numpy 1
//                interp.exec("import numpy\n");
                interp.exec("class myClass():\n    pass\n");
                interp.exec("def createMyClass():\n    return myClass()\n");
                interp.exec("def myTestString():\n    return '123'\n");
                interp.exec("def myTestArray():\n    return [1,2,3]\n");
                interp.exec("def myTestNumber():\n    return 123\n");
                interp.exec("print(myTestNumber())");

// Numpy 2
//                System.out.println("Getting PyCallable");
//                final PyCallable callable = interp.getValue("myClass", PyCallable.class);
//                System.out.println("Calling PyCallable");
//                result = callable.call();
//                System.out.printf("call result: %s%n", result);

// Numpy 3
//                System.out.println("Calling constructor");
//                final PyObject myClass = (PyObject) interp.invoke("myClass");
//                System.out.printf("invoke result: %s%n", myClass);

// Numpy 4
//                interp.exec("_myClass = myClass()");
//                result = interp.getValue("_myClass");
//                System.out.printf("getValue result: %s%n", result);

                System.out.println("Calling function");
                result = interp.invoke("myTestString");
                System.out.printf("invoke result: %s%n", result);
            }
        });
        t.start();
        t.join();

        System.out.println();
        System.err.println();

        t = new Thread(() -> {
            try (Interpreter interp = new SharedInterpreter()) {
                System.out.printf("%nInterpreter: %s%n", interp);
                interp.exec("import numpy as np\n");
                interp.exec("def myTest():\n    return np.array([2, 3, 4])\n");
                interp.exec("print(myTest())");
                Object result = interp.invoke("myTest");
                System.out.printf("invoke result: %s (%s)%n", result, result.getClass().getCanonicalName());
            }
        });
        t.start();
        t.join();
    }
}
