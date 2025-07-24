package net.algart.bridges.jep.tests;

import jep.Interpreter;
import jep.SharedInterpreter;

import java.io.FileNotFoundException;

public class SimpleJepNumpy {
    public static void main(String[] args) throws FileNotFoundException {
        final String root = SimpleJepTest.pythonRoot();
        try (Interpreter interp = new SharedInterpreter()) {
            System.out.println("Interpreter: " + interp);
            System.out.println();
            interp.exec("import sys");
            interp.exec("sys.path.append('" + root + "')");
            // - an alternative to JepConfig.setIncludePath
            interp.exec("from tests.SimpleTestNumpy import demo");
            interp.exec("from java.lang import System\n");
            interp.exec("s = 'Hello World'");
            interp.exec("print(s)"); // - will not work from IDE (see SimpleJepTest.configurePython)
            interp.exec("System.out.println(\"(java:) \" + s)");

            interp.exec("result = demo()");
            interp.exec("print(result)");
            interp.exec("System.out.println(\"(java:) \" + result)");
        }
    }
}
