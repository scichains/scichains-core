export function execute(params, inputs, outputs) {
    const a = inputs.input
    const array = a.getArray()
    const blockLength = a.blockLength()
    let sum = new Array(blockLength)
    for (let i = 0; i < blockLength; i++) {
        sum[i] = 0
    }
    for (let k = 0, n = a.n(), disp = 0; k < n; k++) {
        for (let i = 0; i < blockLength; i++) {
            sum[i] += array[disp++];
        }
    }
    sum.blockLength = blockLength
    return sum
}
