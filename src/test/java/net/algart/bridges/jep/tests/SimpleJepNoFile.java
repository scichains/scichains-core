package net.algart.bridges.jep.tests;

import jep.Interpreter;
import jep.SubInterpreter;

public class SimpleJepNoFile {
    public static void main(String[] args) {
        try (Interpreter interp = new SubInterpreter()) {
            System.out.println("Interpreter: " + interp);
            System.out.println();
            interp.exec("def someClass():\n    pass\n");
            interp.exec("def test():\n    return '123'\n");
            interp.exec("print(test())");
            Object result = interp.invoke("test");
            System.out.printf("From Python: %s%n", result);
        }
    }
}
