package net.algart.bridges.jep.tests;

import jep.Interpreter;
import jep.JepConfig;
import jep.SharedInterpreter;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleJepTest {
    static String pythonRoot() {
        final String root = "src/test/resources/python_tests";
        final Path path = Path.of(root).toAbsolutePath();
        System.out.printf("Using Python home: %s%n", path);
        if (!Files.isDirectory(path)) {
            throw new AssertionError(path + " is not an existing folder!");
        }
        return root;
    }

    static void configurePython(String root) {
        JepConfig config = new JepConfig();
        config.addIncludePaths(root);
        config.redirectStdout(System.out);
        config.redirectStdErr(System.err);
        // - necessary for correct working "print" method from IDE
        SharedInterpreter.setConfig(config);
    }

    public static void main(String[] args) throws FileNotFoundException {
        final String root = pythonRoot();
        configurePython(root);
        try (Interpreter interp = new SharedInterpreter()) {
            System.out.println("Interpreter: " + interp);
            System.out.println();
            interp.exec("from tests import SimpleTest");
            interp.exec("from java.lang import System\n");
            interp.exec("s = 'Hello World'");
            interp.exec("System.out.println(\"(java:) \" + s)");
            interp.exec("print(sys.path)");
            interp.exec("print(s)");
            interp.exec("result = SimpleTest.demo()");
            interp.exec("print(result)");
            interp.exec("System.out.println(\"(java:) \" + result)");
        }
    }
}
