import time
import pyalgart.api as pya

global_counter = 0
print(f"==== Initializing long delay! ====")
# pya.import_file("test.py") # should lead to RuntimeError in a module


def execute(params, inputs, outputs):
    print("Global environment context path: " + str(pya._env.context_path) + " (should be None in a module)")
    global global_counter
    global_counter += 1
    print("Starting loop; counter: " + str(global_counter))
    # All instances of this module in SciChains will use the same counter!
    n = params.delay or 60
    for i in range(1, n + 1):
        info = ""
        if hasattr(params, "_executor"):
            # hasattr is added for testing from the command line
            info = " (" + params._env.context_path + ", session " + params._executor.getSessionId() + ")"
            if params._executor.isInterrupted():
                print("Sleeping interrupted!")
                break
        print(f"{params.title}{info}: {i} from {n} seconds, module counter: {global_counter}")
        time.sleep(1)
    print(f"Done {params.title}: {i} seconds")
    return i

if __name__ == '__main__':
    # you should comment "import pyalgart.api" and its usages to run this test
    from types import SimpleNamespace
    params = SimpleNamespace(delay=10, title="Sleeping")
    inputs = SimpleNamespace()
    outputs = SimpleNamespace()
    execute(params, inputs, outputs)