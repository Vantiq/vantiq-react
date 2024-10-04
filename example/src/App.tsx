import {StyleSheet, View, Button} from 'react-native';
import {init, multiply, add, authWithInternal, authWithOAuth, select, selectOne, count} from 'vantiq-react';


import {useEffect} from 'react';

export default function App() {

    var authenticationState: any = null;

    useEffect(() => {
        // Code to run on component mount
        console.log("INITIALIZING Vantiq Interface");

        let server: string = "http://10.0.0.208:8080";
        let namespace: string = "Scratch1";

        //server = "https://test.vantiq.com";
        //namespace = "SteveNS1";

        init(server, namespace).then(function (authState: any) {
                authenticationState = authState;
                let auth: string = JSON.stringify(authState, null, 3)
                console.log(`init: server=${server} namespace=${namespace} return=${auth}`);
            },
            function (error: any) {
                let err: string = JSON.stringify(error, null, 3);
                console.error(`init FAIL: ${err}`);
            });

        // Optional cleanup function
        return () => {
            console.log("UNMOUNTING Vantiq Interface");
        };
    }, []); //


    const onAdd = () => {
        let a: number = 5;
        let b: number = 7;

        console.log('Invoke Add');

        add(a, b).then(function (value: number) {
                console.log(`Add ${a} + ${b} = ${value}`);
            },
            function (err: string) {
                console.error(`Error ${err}`);
            });
    };

    const onMultiply = () => {
        let a: number = 2;
        let b: number = 6;

        console.log('Invoke Multiply');

        multiply(a, b).then(function (value: number) {
                console.log(`Multiply ${a} * ${b} = ${value}`);
            },
            function (err: string) {
                console.error(`Error ${err}`);
            });
    };

    const onSelect = () => {
        let type: string = "a.b.c.MyType";
        // @ts-ignore
        let props: string[] = ["aaa", "bbb", "ccc", "qqqq", "MyBoolean", "MyObject", "stringAry"];
        // @ts-ignore
        let where: string = null;
        // @ts-ignore
        let sortSpec: string = null;
        let limit: number = 0;

        //where = "{ccc:\"c\"}";
        //sortSpec = "{bbb:1}";

        console.log('Invoke Select');

        select(type, props, where, sortSpec, limit).then(function (results: any) {
                console.log(`select results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onCount = () => {
        let type: string = "a.b.c.MyType";
        // @ts-ignore
        let where: string = null;
        // @ts-ignore

        //where = "{ccc:\"c\"}";

        console.log('Invoke Count');

        count(type,where).then(function (results: any) {
                console.log(`select results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onSelectOne = () => {
        let type: string = "a.b.c.MyType";
        let id:string = "66ff306d033ebc6020bf11ce";

        console.log('Invoke SelectOne');

        selectOne(type, id).then(function (results: any) {
                console.log(`select results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onValidate = () => {

        console.log('Invoke onValidate');

        if (authenticationState.authValid) {
            console.log("Validation: current access token valid")
        }
            //
            //  Either (1) there was no current token, (2) the current token is not valid, (3) the server/namespace changed
        //
        else {
            if (authenticationState.serverType == "internal") {
                let username: string = "steve1";
                let password: string = "x"

                console.log("Validation: INTERNAL")

                authWithInternal(username, password).then(
                    function (newAuthState: any) {
                        authenticationState = newAuthState;
                        let auth: string = JSON.stringify(authenticationState, null, 3)
                        console.log(`Validation: authValid=${authenticationState.authValid} authState=${auth}`);
                    },
                    function (error: any) {
                        console.error(`Validation INTERNAL REJECT error=${JSON.stringify(error)}`)
                    }
                );

            } else if (authenticationState.serverType == "oauth") {
                console.log("Validation: OAUTH");

                authWithOAuth("com.vantiq.mobile", "vantiqMobile").then(
                    function (newAuthState: any) {
                        authenticationState = newAuthState;
                        let auth: string = JSON.stringify(authenticationState, null, 3)
                        console.log(`Validation: authValid=${authenticationState.authValid} authState=${auth}`);
                    },
                    function (error: any) {
                        console.error(`Validation OAUTH REJECT error=${JSON.stringify(error)}`);
                    }
                );
            } else {
                console.error(`Validation FAIL: serverType invalid=${authenticationState.serverType}`);
            }
        }
    };

    return (
        <View style={styles.container}>


            <Button
                title="Click to Multiply"
                color="#00ff00"
                onPress={onMultiply}
            />
            <Button
                title="Click to Add"
                color="#ff0000"
                onPress={onAdd}
            />

            <Button
                title="Validate"
                color="#00ffff"
                onPress={onValidate}
            />

            <Button
                title="Select"
                color="#880088"
                onPress={onSelect}
            />


            <Button
                title="SelectOne"
                color="#cc00cc"
                onPress={onSelectOne}
            />

            <Button
                title="Count"
                color="#220022"
                onPress={onCount}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        rowGap: 20
    },
    box: {
        width: 60,
        height: 60,
        marginVertical: 20,
    },
});
