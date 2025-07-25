export function execute(params, inputs, outputs) {
    const e = params._env.executor
    const msg = "Hello! I am " + e.getSpecification().getName() + ", id " + e.getExecutorId() + " - " + e
    e.showStatus(msg)
    print(msg)
    e.defaultOutputPortName("specification")
    let pl = params._env.platform;
    if (pl != null) {
        pl = pl.jsonString();
    }
    outputs.platform = pl
    outputs.specification = e.getSpecification().jsonString()
}
