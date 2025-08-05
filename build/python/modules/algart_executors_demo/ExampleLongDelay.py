import time

print("==== Initializing long delay! ====")
def execute(params, inputs, outputs):
    n = params.delay or 60
    for i in range(1, n + 1):
        info = ""
        if (hasattr(params, "_executor")):
            # hasattr is added for testing from the command line
            info = " (" + params._executor.getContextPath() + ", session " + params._executor.getSessionId() + ")"
            if (params._executor.isInterrupted()):
                print("Sleeping interrupted!")
                break
        print(f"{params.title}{info}: {i} from {n} seconds")
        time.sleep(1)
    print(f"Done {params.title}: {i} seconds")
    return i

if __name__ == '__main__':
    from types import SimpleNamespace
    params = SimpleNamespace(delay=10, title="Sleeping")
    inputs = SimpleNamespace()
    outputs = SimpleNamespace()
    execute(params, inputs, outputs)