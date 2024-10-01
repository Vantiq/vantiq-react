import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'vantiq-interface-library' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

export const VantiqInterfaceLibrary = NativeModules.VantiqInterfaceLibrary
  ? NativeModules.VantiqInterfaceLibrary
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

export function initialize(server:string,namespace:string): Promise<string> {
  return VantiqInterfaceLibrary.initialize(server,namespace);
}

export function testOne(): Promise<string> {
  return VantiqInterfaceLibrary.testOne();
}
