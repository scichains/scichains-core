import time
import pyalgart.api as pya

def execute(params, inputs, outputs):
    env = pya.env()
    e = env.executor
    # print("Executor: " + str(e))
    msg = (
            "Hello from Python System information!" +
            "\nI am " + e.getSpecification().getName() +
            "\n    id: " + e.getExecutorId() +
            "\n    working in: " + str(env.working_dir) +
            "\n    to string: " + str(e)
    )
    e.showStatus(msg)
    print(msg)
    time.sleep(1)
    e.defaultOutputPortName("specification")
    outputs.current_dir = env.working_dir
    outputs.platform = params._env.platform.jsonString()
    # - alternative way to access environment: params._env
    outputs.specification = e.getSpecification().jsonString()
