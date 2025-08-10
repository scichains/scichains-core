export function execute(params, inputs, outputs) {
    const e = params._env.executor
    const msg = "Hello from JS System information! " +
            "\nI am " + e.getSpecification().getName() +
            "\n    id: " + e.getExecutorId() +
            "\n    working in folder: " + params._env.workingDirectory +
            "\n    executed in chain: " + params._env.contextPath +
            "\n    toString(): " + e;
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
