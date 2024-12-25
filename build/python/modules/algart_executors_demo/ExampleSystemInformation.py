import time

def execute(params, inputs, outputs):
    e = params._sys.executor
    msg = "Hello! I am " + e.getExecutorSpecification().getName() + ", id " + e.getExecutorId() + " - " + str(e)
    e.showStatus(msg)
    print(msg)
    time.sleep(3)
    e.defaultOutputPortName("model")
    outputs.platform = params._sys.platform.jsonString()
    outputs.model = e.getExecutorSpecification().jsonString()
