import {StyleSheet, View, Button} from 'react-native';
import {init, multiply, add, authWithInternal, authWithOAuth, select, selectOne, count,
    insert, update, upsert, deleteOne, deleteWhere, execute, publish} from 'vantiq-react';


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


    const onExecute = () => {
        let procedureName: string = "TestProc";
        let params: any = {
            aaa: 4,
            bbb: 6
        };

        let paramsAsString = JSON.stringify(params);

        console.log('Invoke Execute');

        //procedureName = "NoParms";
        //paramsAsString = null;

        execute(procedureName,  paramsAsString).then(function (results: any) {
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

        let whereAsString = JSON.stringify(where);

        console.log('Invoke Select');

        deleteWhere(type,  whereAsString).then(function (results: any) {
                console.log(`delete results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`delete REJECT error=${JSON.stringify(error, null, 3)}`)
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

    const onInsert = () => {
        let type: string = "a.b.c.MyType";
        let object: any = {
            MyBoolean: false,
            ccc: "CCCC INSERT",
            bbb: "BBBB INSERT",
            aaa: "AAAA INSERT"
        };

        let objectAsString = JSON.stringify(object);

        console.log('Invoke Insert');

        insert(type,objectAsString).then(function (results: any) {
                console.log(`insert results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`insert REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onPublish = () => {
        let topic: string = "/a/b/c";
        let object: any = {
            ccc: "CCCC",
            bbb: "BBBB",
            aaa: "AAAA"
        };

        let objectAsString = JSON.stringify(object);

        console.log('Invoke Publish');

        publish(topic,objectAsString).then(function (results: any) {
                console.log(`publish results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`publish REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };


    const onUpsert = () => {
        let type: string = "a.b.c.MyType";
        let object: any = {
            MyBoolean: false,
            bbb: "NATKEY",
            aaa: "AAAA UPDATE " + Math.random()
        };

        let objectAsString = JSON.stringify(object);

        console.log('Invoke Upsert');

        upsert(type,objectAsString).then(function (results: any) {
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
        let id:string = "6700304b033ebc6020bf1f2f";

        let objectAsString = JSON.stringify(object);

        console.log('Invoke Update1');

        update(type,id,objectAsString).then(function (results: any) {
                console.log(`update results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`update REJECT error=${JSON.stringify(error, null, 3)}`)
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

    const onDeleteOne = () => {
        let type: string = "a.b.c.MyType";
        let id:string = "670416e4a7952f7630942965";

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
                // vantiqReact on staging
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

            <Button
                title="Execute"
                color="#2255dd"
                onPress={onExecute}
            />


            <Button
                title="Publish"
                color="#3388aa"
                onPress={onPublish}
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
