let global_counter = 0;
// - will be global across all imports in the same context
console.log("==== Initializing JS module long delay! ====");


export function execute(params, inputs, outputs) {
    console.log("Global environment context path: " + params._env.context_path);

    global_counter += 1;
    console.log("Starting JS module loop; module counter: " + global_counter);
    // All instances of this module in the same JS context will use the same counter!

    const n = params.delay || 10;
    let i = 0;
    for (i = 1; i <= n; i++) {
        if (params._executor.isInterrupted()) {
            console.log("Sleeping interrupted!");
            break;
        }
        let info = " (" + params._env.context_path + ", session " + params._executor.getSessionId() + ")";
        console.log(`${params.title}${info} in a module: ${i} from ${n} seconds, module counter: ${global_counter}`);
        const Thread = Java.type("java.lang.Thread");
        Thread.sleep(1000);
    }
    console.log(`Done ${params.title}: ${i} seconds`);
    return i - 1;
}
