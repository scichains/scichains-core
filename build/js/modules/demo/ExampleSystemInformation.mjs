export function execute(params, inputs, outputs) {
    const e = params._sys.executor
    const msg = "Hello! I am " + e.executorModel().getName() + ", id " + e.getExecutorId() + " - " + e
    e.showStatus(msg)
    print(msg)
    e.defaultOutputPortName("model")
    let pl = params._sys.platform;
    if (pl != null) {
        pl = pl.jsonString();
    }
    outputs.platform = pl
    outputs.model = e.executorModel().jsonString()
}
