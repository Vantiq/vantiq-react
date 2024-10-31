import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
    `The package 'vantiq-react' doesn't seem to be linked. Make sure: \n\n` +
    Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
    '- You rebuilt the app after installing the package\n' +
    '- You are not using Expo Go\n';

const VantiqReact = NativeModules.VantiqReact
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
    return VantiqReact.multiply(a, b);
}


export function add(a: number, b: number): Promise<number> {
    return VantiqReact.add(a, b);
}


export function init(serverURL: string, namespace: string): Promise<string> {
    return VantiqReact.init(serverURL, namespace);
}

export function authWithOAuth(urlScheme: string, clientId: string): Promise<string> {
    return VantiqReact.authWithOAuth(urlScheme, clientId);
}

export function serverType(): Promise<string> {
    return VantiqReact.serverType();
}

export function verifyAuthToken(): Promise<string> {
    return VantiqReact.verifyAuthToken();
}

export function authWithInternal(username: string, password: string): Promise<string> {
    return VantiqReact.authWithInternal(username, password);
}

export function select(type: string, props: string[], where: any, sortSpec: any, limit: number): Promise<string> {
    return VantiqReact.select(type, props, where, sortSpec, limit);
}

export function selectOne(type: string, id: string): Promise<string> {
    return VantiqReact.selectOne(type, id);
}

export function count(type: string, where: any): Promise<string> {
    return VantiqReact.count(type, where);
}

export function insert(type: string, object: any): Promise<string> {
    return VantiqReact.insert(type, object);
}

export function update(type: string, id:string, object: any): Promise<string> {
    return VantiqReact.update(type, id, object);
}

export function upsert(type: string, object: any): Promise<string> {
    return VantiqReact.upsert(type, object);
}

export function deleteWhere(type: string, where: any): Promise<string> {
    return VantiqReact.delete(type, where);
}

export function deleteOne(type: string, id: string): Promise<string> {
    return VantiqReact.deleteOne(type, id);
}

export function executeByPosition(procedureName: string, params: any[]): Promise<string> {
    return VantiqReact.executeByPosition(procedureName, params);
}

export function executeByName(procedureName: string, params: any): Promise<string> {
    return VantiqReact.executeByName(procedureName, params);
}

export function executeStreamedByPosition(procedureName: string, params: any[], progressEvent:string): Promise<string> {
    return VantiqReact.executeStreamedByPosition(procedureName, params, progressEvent);
}

export function executeStreamedByName(procedureName: string, params: any, progressEvent:string): Promise<string> {
    return VantiqReact.executeStreamedByName(procedureName, params, progressEvent);
}

export function publish(topic: string, object: any): Promise<string> {
    return VantiqReact.publish(topic, object);
}

export function publishEvent(resource: string, resourceId:string, object: any): Promise<string> {
    return VantiqReact.publishEvent(resource, resourceId, object);
}

export function createInternalUser(username:string, password:string, email:string, firstName:string, lastName:string, phone:string): Promise<string> {
    return VantiqReact.createInternalUser(username, password, email, firstName, lastName, phone);
}

export function createOAuthUser(urlScheme: string, clientId: string): Promise<string> {
    return VantiqReact.createOAuthUser(urlScheme, clientId);
}

export function registerForPushNotifications(): Promise<string> {
	return VantiqReact.registerForPushNotifications();
}

