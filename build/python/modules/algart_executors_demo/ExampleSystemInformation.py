import time

def execute(params, inputs, outputs):
    e = params._env.executor
    msg = (
            "Hello! I am " + e.getSpecification().getName() +
            "\n    id: " + e.getExecutorId() +
            "\n    working in: " + str(params._env.working_dir) +
            "\n    to string: " + str(e)
    )
    e.showStatus(msg)
    print(msg)
    time.sleep(3)
    e.defaultOutputPortName("model")
    outputs.platform = params._env.platform.jsonString()
    outputs.model = e.getSpecification().jsonString()
