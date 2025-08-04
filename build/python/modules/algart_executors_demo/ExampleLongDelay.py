import time

print("==== Initializing long delay! ====")
def execute(params, inputs, outputs):
    n = params.delay or 60
    for i in range(1, n + 1):
        if (hasattr(params, "_executor") and params._executor.isInterrupted()):
            # hasattr is added for testing from the command line
            print("Sleeping interrupted!")
            break
        print(f"{params.title}: {i} from {n} seconds")
        time.sleep(1)
    print(f"Done {params.title}: {i} seconds")
    return i

if __name__ == '__main__':
    from types import SimpleNamespace
    params = SimpleNamespace(delay=10, title="Sleeping")
    inputs = SimpleNamespace()
    outputs = SimpleNamespace()
    execute(params, inputs, outputs)