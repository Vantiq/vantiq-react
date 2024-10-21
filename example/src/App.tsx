import {StyleSheet, View, Button} from 'react-native';
import {
    init, multiply, add, authWithInternal, authWithOAuth, select, selectOne, count,
    insert, update, upsert, deleteOne, deleteWhere, executeByName, executeByPosition, publishEvent, verifyAuthToken
} from 'vantiq-react';

import {useEffect} from 'react';

const VantiqInternal: string = "Internal";
const VantiqOAuth: string = "OAuth";


const VantiqServer: string = "http://10.0.0.208:8080";
const VantiqNamespace: string = "Scratch1";
const internalUsername: string = "steve1";
const internalPassword: string = "x";
// @ts-ignore
const OAuthClientId: string = null;
// @ts-ignore
const OAUthUrlScheme: string = null;


/*
const VantiqServer:string = "https://test.vantiq.com";
const VantiqNamespace:string = "Scratch1";
// @ts-ignore
const internalUsername:string = null;
// @ts-ignore
const internalPassword:string = null;
const OAuthClientId = "vantiqMobile";
const OAUthUrlScheme = "com.vantiq.mobile";
*/

/*
const VantiqServer:string = "https://staging.vantiq.com";
const VantiqNamespace:string = "MySandbox";
// @ts-ignore
const internalUsername:string = null;
// @ts-ignore
const internalPassword:string = null;
const OAuthClientId = "vantiqReact";
const OAUthUrlScheme = "vantiqreact";
*/

export default function App() {

    var authenticationState: any = null;

    useEffect(() => {
        // Code to run on component mount
        console.log("INITIALIZING Vantiq Interface");
        console.log(`VantiqServer=${VantiqServer}`);
        console.log(`VantiqNamespace=${VantiqNamespace}`);
        console.log(`internalUsername=${internalUsername}`);
        console.log(`internalPassword=${internalPassword}`);
        console.log(`OAUthUrlScheme=${OAUthUrlScheme}`);
        console.log(`OAuthClientId=${OAuthClientId}`);

        init(VantiqServer, VantiqNamespace).then(function (authState: any) {
                authenticationState = authState;
                let auth: string = JSON.stringify(authState, null, 3)
                console.log(`init: server=${VantiqServer} namespace=${VantiqNamespace} return=${auth}`);
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


    const onSelect = () => {
        let type: string = "a.b.c.MyType";
        let props: string[] = ["aaa", "bbb", "ccc", "qqqq", "MyBoolean", "MyObject", "stringAry"];
        let where: any = {
            ccc: "c"
        };
        let sortSpec: any = {
            bbb: 1
        };
        let limit: number = 100;

        where = null;
        sortSpec = null;

        console.log('Invoke Select');

        select(type, props, where, sortSpec, limit).then(function (results: any) {
                console.log(`select results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };


    const onExecuteByName = () => {
        let procedureName: string = "TestProc";
        console.log('Invoke Execute By Name');

        let params: any = {
            aaa: 4,
            bbb: 6
        };

        executeByName(procedureName, params).then(function (results: any) {
                console.log(`execute results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`execute REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onExecuteByPosition = () => {
        let procedureName: string = "TestProc";
        console.log('Invoke Execute By Position');


        let params: any = [15, 7];
        executeByPosition(procedureName, params).then(function (results: any) {
                console.log(`execute results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`execute REJECT error=${JSON.stringify(error, null, 3)}`)
            })

    };


    const onDelete = () => {
        let type: string = "a.b.c.MyType";
        // @ts-ignore
        let where: string = {
            bbb: "b2"
        };

        console.log('Invoke Select');

        deleteWhere(type, where).then(function (results: any) {
                console.log(`delete results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`delete REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onCount = () => {
        let type: string = "a.b.c.MyType";
        let where: any = {
            ccc: "c"
        };

        where = null;

        console.log('Invoke Count');

        count(type, where).then(function (results: any) {
                console.log(`select results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onInsert = () => {
        let type: string = "a.b.c.MyType";
        let object: any = {
            MyBoolean: false,
            ccc: "CCCC INSERT",
            bbb: "BBB INSERT " + Math.random(),
            aaa: "AAAA INSERT"
        };

        console.log('Invoke Insert');

        insert(type, object).then(function (results: any) {
                console.log(`insert results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`insert REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onPublish = () => {
        let resourceId: string = "a.b.c.MyService/VEH";
        let resource: string = "services";
        let object: any = {
            ccc: "CCCC",
            bbb: "BBBB",
            aaa: "AAAA"
        };

        console.log('Invoke Publish');

        publishEvent(resource, resourceId, object).then(function (results: any) {
                console.log(`publishEvent results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`publishEvent REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };


    const onUpsert = () => {
        let type: string = "a.b.c.MyType";
        let object: any = {
            MyBoolean: false,
            bbb: "NATKEY",
            aaa: "AAAA UPDATE " + Math.random()
        };

        console.log('Invoke Upsert');

        upsert(type, object).then(function (results: any) {
                console.log(`upsert results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`upsert REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };


    const onUpdate = () => {
        let type: string = "a.b.c.MyType";
        let object: any = {
            MyBoolean: false,
            ccc: "CCCC UPDATE " + Math.random()
        };
        let id: string = "6700304b033ebc6020bf1f2f";

        console.log('Invoke Update1');

        update(type, id, object).then(function (results: any) {
                console.log(`update results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`update REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onSelectOne = () => {
        let type: string = "a.b.c.MyType";
        let id: string = "66ff306d033ebc6020bf11ce";

        console.log('Invoke SelectOne');

        selectOne(type, id).then(function (results: any) {
                console.log(`select results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onDeleteOne = () => {
        let type: string = "a.b.c.MyType";
        let id: string = "670416e4a7952f7630942965";

        console.log('Invoke DeleteOne');

        deleteOne(type, id).then(function (results: any) {
                console.log(`deleteOne results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`deleteOne REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onValidate = () => {

        console.log('Invoke onValidate');

        authenticationState.authValid = false;

        if (authenticationState.authValid) {
            console.log("Validation: current access token valid")
        }
            //
            //  Either (1) there was no current token, (2) the current token is not valid, (3) the server/namespace changed
        //
        else {
            if (authenticationState.serverType == "Internal") {
                let username: string = internalUsername;
                let password: string = internalPassword;

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

            } else if (authenticationState.serverType == VantiqOAuth) {
                console.log("Validation: OAUTH");
                // vantiqReact on staging
                authWithOAuth(OAUthUrlScheme, OAuthClientId).then(
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

    const onRefresh = () => {

        console.log('Invoke onRefresh');

        authenticationState.authValid = false;

        if (authenticationState.serverType == VantiqInternal) {
            console.error(`Can't Refresh Internal`);

        } else if (authenticationState.serverType == VantiqOAuth) {
            console.log("Refesh: OAUTH");
            // vantiqReact on staging
            verifyAuthToken().then(
                function (newAuthState: any) {
                    authenticationState = newAuthState;
                    let auth: string = JSON.stringify(authenticationState, null, 3)
                    console.log(`Validation: authValid=${authenticationState.authValid} authState=${auth}`);
                },
                function (error: any) {
                    console.error(`Validation OAUTH REJECT error=${JSON.stringify(error)}`);
                }
            );
        }

    };

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

    return (
        <View style={styles.container}>




            <View style={styles.navButtons}>
                <Button
                    title="Validate"
                    color="#00ffff"
                    onPress={onValidate}
                />
                <Button
                    title="Refresh"
                    color="#aa0000"
                    onPress={onRefresh}
                />
            </View>
            <View style={styles.navButtons}>

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
            <View style={styles.navButtons}>
                <Button
                    title="Insert"
                    color="#338833"
                    onPress={onInsert}
                />

                <Button
                    title="Update"
                    color="#882288"
                    onPress={onUpdate}
                />

                <Button
                    title="Upsert"
                    color="#6622dd"
                    onPress={onUpsert}
                />
            </View>

            <View style={styles.navButtons}>
                <Button
                    title="Delete"
                    color="#6622dd"
                    onPress={onDelete}
                />

                <Button
                    title="Delete One"
                    color="#6622dd"
                    onPress={onDeleteOne}
                />
            </View>

            <View style={styles.navButtons}>

                <Button
                    title="Execute By Name"
                    color="#2255dd"
                    onPress={onExecuteByName}
                />

                <Button
                    title="Execute By Position"
                    color="#2255dd"
                    onPress={onExecuteByPosition}
                />
            </View>


            <Button
                title="Publish"
                color="#3388aa"
                onPress={onPublish}
            />

            <View style={styles.navButtons}>
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
            </View>
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
    navButtons: {
        flexDirection: 'row',
        columnGap: 16
    },
});
