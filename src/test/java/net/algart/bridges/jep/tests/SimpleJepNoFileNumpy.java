package net.algart.bridges.jep.tests;

import jep.Interpreter;
import jep.SharedInterpreter;
import jep.SubInterpreter;

public class SimpleJepNoFileNumpy {
    public static void main(String[] args) {
        try (Interpreter interp = new SharedInterpreter()) {
            System.out.println("Interpreter: " + interp);
            System.out.println();
            interp.exec("import numpy as np");
            interp.exec("import sys");
            interp.exec("from java.lang import System");
            interp.exec("s = 'Hello World'");
            interp.exec("System.out.println(\"(java:) \" + s)");
            interp.exec("print(sys.path)");
            interp.exec("print(s)");
            // - note: native "print" operator is visible only in the command line, not from IDEA
            interp.exec("print(s[1:3]+'!')");
            interp.exec("a = np.array([2, 3, 4])");
            interp.exec("System.out.println(\"(java:) \" + np.array_str(a))");
        }
    }
}
