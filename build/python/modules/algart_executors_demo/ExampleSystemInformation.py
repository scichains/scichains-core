import time

def execute(params, inputs, outputs):
    e = params._sys.executor
    msg = (
            "Hello! I am " + e.getSpecification().getName() +
            "\n    id: " + e.getExecutorId() +
            "\n    working in: " + str(params._sys.working_dir) +
            "\n    to string: " + str(e)
    )
    e.showStatus(msg)
    print(msg)
    time.sleep(3)
    e.defaultOutputPortName("model")
    outputs.platform = params._sys.platform.jsonString()
    outputs.model = e.getSpecification().jsonString()
