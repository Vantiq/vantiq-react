import {NativeModules, Platform} from 'react-native';

const LINKING_ERROR =
    `The package 'vantiq-interface-library' doesn't seem to be linked. Make sure: \n\n` +
    Platform.select({ios: "- You have run 'pod install'\n", default: ''}) +
    '- You rebuilt the app after installing the package\n' +
    '- You are not using Expo Go\n';

export const VantiqInterfaceLibrary = NativeModules.VantiqReact
    ? NativeModules.VantiqReact
    : new Proxy(
        {},
        {
            get() {
                throw new Error(LINKING_ERROR);
            },
        }
    );

export function multiply(a: number, b: number): Promise<number> {
    return VantiqInterfaceLibrary.multiply(a, b);
}

export function add(a: number, b: number): Promise<number> {
    return VantiqInterfaceLibrary.add(a, b);
}


export function init(server: string, namespace: string): Promise<string> {
    return VantiqInterfaceLibrary.init(server, namespace);
}

export function authWithOAuth(redirectURL: string, clientId: string): Promise<string> {
    return VantiqInterfaceLibrary.authWithOAuth(redirectURL, clientId);
}

export function authWithInternal(username: string, password: string): Promise<string> {
    return VantiqInterfaceLibrary.authWithInternal(username, password);
}

export function select(type: string, props: string[], where: string, sortSpec: string, limit: number): Promise<string> {
    return VantiqInterfaceLibrary.select(type, props, where, sortSpec, limit);
}

export function selectOne(type: string, id: string): Promise<string> {
    return VantiqInterfaceLibrary.selectOne(type, id);
}

export function count(type: string, where: string): Promise<string> {
    return VantiqInterfaceLibrary.count(type, where);
}

export function insert(type: string, object: string): Promise<string> {
    return VantiqInterfaceLibrary.insert(type, object);
}

export function update(type: string, id:string, object: string): Promise<string> {
    return VantiqInterfaceLibrary.update(type, id, object);
}

export function upsert(type: string, object: string): Promise<string> {
    return VantiqInterfaceLibrary.upsert(type, object);
}


export function deleteWhere(type: string, where: string): Promise<string> {
    return VantiqInterfaceLibrary.delete(type, where);
}


export function deleteOne(type: string, id: string): Promise<string> {
    return VantiqInterfaceLibrary.deleteOne(type, id);
}

export function execute(procedureName: string, params: string): Promise<string> {
    return VantiqInterfaceLibrary.execute(procedureName, params);
}

export function publish(topic: string, object: string): Promise<string> {
    return VantiqInterfaceLibrary.publish(topic, object);
}
