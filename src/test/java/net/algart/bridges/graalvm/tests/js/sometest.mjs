import { toJson, toJsonString } from "./jsons.mjs"

export function test(o) {
    console.log("versionECMAScript: " + Graal.versionECMAScript)
    print("Object type: " + typeof(o));
    print("Java String form: " + java.lang.String.valueOf(o));
    print("Object: " + o);
    return "JSON form: " +  toJsonString(o);
}

// console.log(test([1,2,3,4,5]));
export function simpleTest() {
    const o = [1, 2, 3]
    print("Object type: " + typeof(o));
    return "JSON form: " +  toJsonString(o);
}
simpleTest;