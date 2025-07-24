package net.algart.bridges.jep.tests;

import jep.Interpreter;
import jep.SharedInterpreter;

public class SimpleJepForOperationsSpeed {
    public static void main(String[] args) {
        final String root = SimpleJepTest.pythonRoot();
        SimpleJepTest.configurePython(root);
        try (Interpreter interp = new SharedInterpreter()) {
            interp.exec("from java.lang import System\n");
            interp.exec("print(sys.path)\n");
            interp.exec("import tests.SimpleOperationsSpeed as Speed");
            interp.exec("Speed.SimpleOperationSpeed().testAll()");
            interp.exec("System.out.println(\"Ok\")\n");
        }
    }
}
