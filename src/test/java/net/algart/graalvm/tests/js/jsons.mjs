export function toJson(o) {
    if (o == null) {
        return o;
    }
    if (isCollection(o)) {
        return collectionToJson(o);
    }
    if (isMap(o)) {
        return mapToJson(o);
    }
    if (typeof o !== 'object') {
        return o;
    }
    const result = new Object();
    for (const fieldName of Object.getOwnPropertyNames(o)) {
        const e = o[fieldName];
        result[fieldName] = toJson(e);
    }
    return result;
}

export function toJsonString(o) {
    return JSON.stringify(toJson(o), null, 2);
}

function isCollection(a) {
    return a instanceof Array || a instanceof Set;
}

function collectionToJson(a) {
    const result = [];
    for (let e of a) {
        result.push(toJson(e));
    }
    return result;
}

function isMap(m) {
    return m instanceof Map;
}

function mapToJson(m) {
    const result = {};
    for (let [key, value] of m) {
        result[key] = toJson(value);
    }
    return result;
}