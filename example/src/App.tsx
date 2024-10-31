import {useEffect, useState} from 'react'
import { NativeModules, StyleSheet, Pressable, Text, View, TextInput, ScrollView, Button } from "react-native";
import { RootSiblingParent } from 'react-native-root-siblings';
import {NativeEventEmitter} from 'react-native';

import {init, authWithInternal, authWithOAuth, select, selectOne, count, createInternalUser, createOAuthUser,
    insert, update, upsert, deleteOne, deleteWhere, executeByName, executeByPosition, publish, publishEvent,
    verifyAuthToken, executeStreamedByName, executeStreamedByPosition, registerForPushNotifications,
    registerSupportedEvents} from 'vantiq-react';

const {VantiqReact} = NativeModules;

const VANTIQ_SERVER:string = 'https://staging.vantiq.com';
const VANTIQ_NAMESPACE:string = 'react'
const FORCELOGIN:boolean = true;  //  Normal scenario where we automatically establish a login

//const VANTIQ_SERVER:string = 'http://10.0.0.208:8080';
//const VANTIQ_NAMESPACE:string = 'Scratch1';
//const FORCELOGIN:boolean = true; //  Normal scenario where we automatically establish a login

const VantiqInternal:string = "Internal";
const VantiqOAuth:string = "OAuth";
const OAuthClientId = "vantiqReact";
const OAUthUrlScheme = "vantiqreact";

let transcriptText:string = "";
let authenticationState:any;

export default function Index() {
    const [transcript, setTranscript] = useState(transcriptText);
    const [uiType, setUIType] = useState('suite');
    const [authVisible, setAuthVisible] = useState(false);
    const [startVisible, setStartVisible] = useState(true);
    const [runningSuite, setRunningSuite] = useState(false);
    const [internalUsername, setInternalUsername] = useState('');
    const [internalPassword, setInternalPassword] = useState('');
    
    useEffect(() => {
        init(VANTIQ_SERVER, VANTIQ_NAMESPACE).then(
          function(response:any) {
             authenticationState = response;

             if (response.authValid) {
                 setStartVisible(true);
                 registerForPushNotifications();
             } else if (FORCELOGIN)
             {
                 // authentication error so need to authenticate
                 if (response.serverType == VantiqInternal) {
                     setAuthVisible(true);
                 } else if (response.serverType == VantiqOAuth) {
                     VantiqReact.authWithOAuth(OAUthUrlScheme, OAuthClientId).then(
                        function(response:any) {
                           if (response.authValid) {
                               setStartVisible(true);
                               registerForPushNotifications();
                           } else {
                               addToTranscript('Authentication error: ' + response.errorStr);
                           }
                        }, function(error:any) {
                           addToTranscript('Authentication error: ' + error.errorStr);
                        });
                 } else {
                     addToTranscript('Invalid server type');
                 }
             }
             else
             {
                 console.log("Not Authenticated");
             }
           }, function(error:any) {
             addToTranscript('init fail: ' + error.errorStr);
           });

        VantiqReact.registerSupportedEvents(["pushNotification", "TestExecuteStreamedByName", "TestExecuteStreamedByPosition"]);
        const eventEmitter = new NativeEventEmitter(NativeModules.VantiqReact);
        let eventListener1 = eventEmitter.addListener("TestExecuteStreamedByName", event => {
            console.log(JSON.stringify(event,null,3)) // "someValue"
        });
        let eventListener2 = eventEmitter.addListener("TestExecuteStreamedByPosition", event => {
            console.log(JSON.stringify(event,null,3)) // "someValue"
        });
        let notifyListener = eventEmitter.addListener("pushNotification", event => {
            addToTranscript("Received notification: type = " + event.type);
        });
        // Removes the event listeners once unmounted
        return () => {
            eventListener1.remove();
            eventListener2.remove();
            notifyListener.remove();
        };
        
    }, []);

    // user presses the Start Running button
    function onButtonPress() {
        setStartVisible(false);
        runTests();
    }
    
    // user presses one of the Test Mode buttons
    function onModePress(mode:any) {
        setUIType(mode);
    }
    
    // used for logging in to an Internal auth server, after the user has entered
    // a username and password (internalUsername, internalPassword)
    function onLoginPress() {
        setAuthVisible(false);
        VantiqReact.authWithInternal(internalUsername, internalPassword).then(
            function(response:any) {
               if (response.authValid) {
                   registerForPushNotifications();
                   runTests();
               } else {
                   addToTranscript('Authentication error: ' + response.errorStr);
                   setAuthVisible(true);
               }
            }, function(error:any) {
                setAuthVisible(true);
                addToTranscript('Authentication error: ' + error.message + ", code: " + error.code);
            });
    }
    
    // helper to register for push notifications, called after authenticating
    function registerForPushNotifications() {
        VantiqReact.registerForPushNotifications().then(
          function() {
              addToTranscript('Registered for Push Notifications');
          }, function(error:any) {
              reportTestError('Register for Push Notifications', error);
          });
    }
    
    // helper to add text to the suite transcript
    const addToTranscript = (text:string) => {
        transcriptText += text + '\n';
        setTranscript(transcriptText);
    }
    
    // helper to add any error string and code to the transcript
    const reportTestError = (op:string, error:any) => {
        addToTranscript(op + ' error: ' + error.message + ", code: " + error.code);
    }
    
    // test harness for running in suite mode
    let lastVantiqID:string;
    const runTests = () => {
        setRunningSuite(true);
        VantiqReact.select('system.types', [], null, {}, -1).then(
            function(data:any) {
                let length:number = data ? data.length : "N/A";
                addToTranscript('Select returns ' + length + ' items.');
            }, function(error:any) {
                reportTestError('Select', error);
            });
        setTimeout(function() {
            VantiqReact.count('system.types', {"ars_version":{"$gt":5}}).then(
                 function(count:any) {
                     addToTranscript('Count returns ' + count + ' items.');
                 }, function(error:any) {
                     reportTestError('Count', error);
                 });
        }, 2000);
        setTimeout(function() {
            VantiqReact.insert('TestType', {"intValue":42,"uniqueString":"42", boolValue:true}).then(
                function(data:any) {
                    data = data ? data : {};
                    lastVantiqID = (data._id).toString();
                    addToTranscript('Insert successful, id: ' + lastVantiqID + '.');
                }, function(error:any) {
                    reportTestError('Insert', error);
                });
        }, 4000);
        setTimeout(function() {
            VantiqReact.upsert('TestType', {"intValue":44,"uniqueString":"A Unique String."}).then(
                function(_data:any) {
                    addToTranscript('Upsert successful.');
                }, function(error:any) {
                    reportTestError('Upsert', error);
                });
        }, 6000);
        setTimeout(function() {
            VantiqReact.upsert('TestType', {"intValue":45,"uniqueString":"A Unique String."}).then(
                function(_data:any) {
                    addToTranscript('Upsert successful.');
                }, function(error:any) {
                    reportTestError('Upsert', error);
                });
        }, 8000);
        setTimeout(function() {
            VantiqReact.update('TestType', lastVantiqID, {"stringValue":"Updated String."}).then(
                function(_data:any) {
                    addToTranscript('Update successful.');
                }, function(error:any) {
                    reportTestError('Update', error);
                });
        }, 10000);
        setTimeout(function() {
            VantiqReact.selectOne('TestType', lastVantiqID).then(
                function(data:any) {
                    data = data ? data : [];
                    addToTranscript('SelectOne successful: ' + data.length + ' records.');
                }, function(error:any) {
                    reportTestError('SelectOne', error);
                });
        }, 12000);
        setTimeout(function() {
            VantiqReact.publish('/vantiq', {"intValue":42}).then(
                function(_data:any) {
                    addToTranscript('Publish successful.');
                }, function(error:any) {
                    reportTestError('Publish', error);
                });
        }, 14000);
        setTimeout(function() {
            VantiqReact.executeByName('sumTwo', {"val1":35, "val2":21}).then(
                function(data:any) {
                    addToTranscript('ExecuteByName successful: ' + JSON.stringify(data));
                }, function(error:any) {
                    reportTestError('ExecuteByName', error);
                });
        }, 16000);
        setTimeout(function() {
            VantiqReact.executeByPosition('sumTwo', [35, 21]).then(
                function(data:any) {
                    addToTranscript('ExecuteByPosition successful: ' + JSON.stringify(data));
                }, function(error:any) {
                    reportTestError('ExecuteByPosition', error);
                });
        }, 18000);
        setTimeout(function() {
            VantiqReact.deleteOne('TestType', lastVantiqID).then(
                function(_data:any) {
                    addToTranscript('DeleteOne successful.');
                }, function(error:any) {
                    reportTestError('DeleteOne', error);
                });
        }, 20000);
        setTimeout(function() {
            VantiqReact.delete('TestType', {"intValue":42}).then(
                function(_data:any) {
                    addToTranscript('Delete successful.');
                }, function(error:any) {
                    reportTestError('Delete', error);
                });
        }, 22000);
        setTimeout(function() {
            VantiqReact.select('system.types', ["name", "_id"], {}, {"name":-1}, -1).then(
                function(data:any) {
                    let length:number = data ? data.length : "N/A";
                    addToTranscript('Select returns ' + length + ' items.');
                }, function(error:any) {
                    reportTestError('Select', error);
                });
        }, 24000);
        setTimeout(function() {
            VantiqReact.delete('TestType', {"intValue":42}).then(
                function(_data:any) {
                    addToTranscript('Delete successful.');
                    addToTranscript('Tests complete.');
                    setStartVisible(true);
                    setRunningSuite(false);
                }, function(error:any) {
                    reportTestError('Delete', error);
                    addToTranscript('Tests complete.');
                    setStartVisible(true);
                    setRunningSuite(false);
                });
        }, 26000);
    }

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

        let params: any = [10, 1000];
        executeByPosition(procedureName, params).then(function (results: any) {
                console.log(`execute results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`execute REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };
    

    const onExecuteStreamedByName = () => {
        let procedureName: string = "TestStreamedProc";
        console.log('Invoke Execute Streamed By Name');

        let params: any =  {
            nnn:30,
            delay:1000
        };
        
        executeStreamedByName(procedureName, params, "TestExecuteStreamedByName").then(
            function (results: any)
            {
                console.log(`execute results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any)
            {
                console.error(`execute REJECT error=${JSON.stringify(error, null, 3)}`)
            });
    };

    const onExecuteStreamedByPosition = () => {
        let procedureName: string = "TestStreamedProc";
        console.log('Invoke Execute Streamed By Position');

        let params: any = [30, 1000];
        executeStreamedByPosition(procedureName, params, "TestExecuteStreamedByPosition").then(function (results: any)
            {
                console.log(`execute results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`execute REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };
    const onCreateInternalUser = () => {
        console.log('Invoke CreateInternalUser');

        createInternalUser("joeUser1","x", "scl1954@gmail.com","Steve","Langley","930-6458").then(function (results: any) {
                console.log(`createInternalUser results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`createInternalUser REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onCreateOAuthUser = () => {
        console.log('Invoke onCreateOAuthUser');

        createOAuthUser(OAUthUrlScheme, OAuthClientId).then(function (results: any) {
                console.log(`createOAuthUser results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`createOAuthUser REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };
    
    const onDelete = () => {
        let type: string = "a.b.c.MyType";
        let object: any = {
            MyBoolean: false,
            ccc: "CCC DELETE WHERE",
            bbb: "BBB DELETE WHERE " + Math.random(),
            aaa: "AAA DELETE WHERE"
        };

        console.log('Invoke Insert to delete-where a record');

        insert(type, object).then(function (results: any)
            {
                console.log(`insert results=${JSON.stringify(results, null, 3)}`);
                console.log('Invoke DeleteWhere to delete record');

                let where: any = {
                    ccc: "CCC DELETE WHERE"
                };

                console.log('Invoke Select');

                deleteWhere(type, where).then(function (results: any) {
                        console.log(`delete results=${JSON.stringify(results, null, 3)}`)
                    },
                    function (error: any) {
                        console.error(`delete REJECT error=${JSON.stringify(error, null, 3)}`)
                    })
            },
            function (error: any) {
                console.error(`insert REJECT error=${JSON.stringify(error, null, 3)}`)
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
        let topic: string = "/aaa/bbb/ccc";
        let object: any = {
            ccc: "CCCC",
            bbb: "BBBB",
            aaa: "AAAA"
        };

        console.log('Invoke Publish');

        publish(topic, object).then(function (results: any) {
                console.log(`publish results=${JSON.stringify(results, null, 3)}`)
            },
            function (error: any) {
                console.error(`publish REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onPublishEvent = () => {
        let resourceId: string = "a.b.c.MyService/VEH";
        let resource: string = "services";
        let object: any = {
            ccc: "CCCC",
            bbb: "BBBB",
            aaa: "AAAA"
        };

        console.log('Invoke Publish Event');

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
            aaa: "AAAA UPSERT " + Math.random()
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
            ccc: "CCC INSERTED TO TEST UPDATE",
            bbb: "BBB INSERTED TO TEST UPDATE " + Math.random(),
            aaa: "AAA INSERTED TO TEST UPDATE"
        };

        console.log('Invoke Insert to add a record');

        insert(type, object).then(function (results: any)
            {
                console.log(`insert results=${JSON.stringify(results, null, 3)}`);

                object = results;
                let id: string = object._id;
                object.ccc = object.ccc += " - UPDATED";
                delete object._id;

                console.log('Invoke Update to update record ' + id);

                update(type, id, object).then(function (results: any) {
                        console.log(`update results=${JSON.stringify(results, null, 3)}`)
                    },
                    function (error: any) {
                        console.error(`update REJECT error=${JSON.stringify(error, null, 3)}`)
                    })
            },
            function (error: any) {
                console.error(`insert REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onSelectOne = () => {
        let type: string = "a.b.c.MyType";
        // @ts-ignore
        let props: string[] = null;
        let where: any = {
            bbb: "b"
        };
        let sortSpec: any = null;
        let limit: number = 100;

        console.log("Invoke Select for object with 'bbb'='b'");

        select(type, props, where, sortSpec, limit).then(function (results: any)
            {
                console.log(`select results=${JSON.stringify(results, null, 3)}`)

                if (results.length == 1)
                {
                    let id:string = results[0]._id;
                    console.log('Invoke SelectOne with id=' + id);

                    selectOne(type, id).then(function (results: any) {
                            let resultObj:any = null;
                            if (results && (results.length == 1))
                            {
                                resultObj = results[0];
                            }
                            console.log(`selectOne results=${JSON.stringify(resultObj, null, 3)}`)
                        },
                        function (error: any) {
                            console.error(`selectOne REJECT error=${JSON.stringify(error, null, 3)}`)
                        })
                }
                else
                {
                    console.log(`Expected record was missing`);
                }

            },
            function (error: any) {
                console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
            })
    };

    const onDeleteOne = () => {
        let type: string = "a.b.c.MyType";
        let object: any = {
            MyBoolean: false,
            ccc: "CCC DELETE ONE",
            bbb: "BBB DELETE ONE " + Math.random(),
            aaa: "AAA DELETE ONE"
        };

        console.log('Invoke Insert to add a record');

        insert(type, object).then(function (results: any)
            {
                console.log(`insert results=${JSON.stringify(results, null, 3)}`);

                let id: string = results._id;

                console.log('Invoke DeleteOne to delete record ' + id);

                deleteOne(type, id).then(function (results: any) {
                        console.log(`deleteOne results=${JSON.stringify(results, null, 3)}`)
                    },
                    function (error: any) {
                        console.error(`deleteOne REJECT error=${JSON.stringify(error, null, 3)}`)
                    })
            },
            function (error: any) {
                console.error(`insert REJECT error=${JSON.stringify(error, null, 3)}`)
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

    // @ts-ignore
    // @ts-ignore
    return (
        <RootSiblingParent>
            <View style={styles.container}>
                <Text style={styles.text}>Vantiq React Sample</Text>
                {!runningSuite ?
                    (<View style={{flexDirection:"row", justifyContent:'center'}}>
                        <View style={{marginTop:7}}>
                            <Text style={styles.text}>Test Mode:</Text>
                        </View>
                        <View>
                            <Pressable style={styles.modeButton} onPress={()=>onModePress('suite')}>
                                <Text style={styles.text}>Suite</Text>
                            </Pressable>
                        </View>
                        <View>
                            <Pressable style={styles.modeButton} onPress={()=>onModePress('single')}>
                                <Text style={styles.text}>Single</Text>
                            </Pressable>
                        </View>
                     </View>) : null
                }
                {(uiType=='suite') && startVisible ?
                    (<Pressable style={styles.button} onPress={onButtonPress}>
                        <Text style={styles.text}>Start Tests</Text>
                    </Pressable>) : null
                }
                {(uiType=='single') ?
                    (
                        <View style={styles.container1}>
                            <View style={styles.navButtons}>
                                <Button
                                    title="Validate"
                                    color="#aa0000"
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
                                    color="#880088"
                                    onPress={onSelectOne}
                                />
                                <Button
                                    title="Count"
                                    color="#880088"
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
                                    color="#338833"
                                    onPress={onUpdate}
                                />
                                <Button
                                    title="Upsert"
                                    color="#338833"
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
                            <View style={styles.navButtons}>

                                <Button
                                    title="Streamed By Name"
                                    color="#2255dd"
                                    onPress={onExecuteStreamedByName}
                                />
                                <Button
                                    title="Streamed By Position"
                                    color="#2255dd"
                                    onPress={onExecuteStreamedByPosition}
                                />
                            </View>

                            <View style={styles.navButtons}>
                                <Button
                                    title="Publish"
                                    color="#3388aa"
                                    onPress={onPublish}
                                />
                                <Button
                                    title="Publish Event"
                                    color="#3388aa"
                                    onPress={onPublishEvent}
                                />
                            </View>

                            <View style={styles.navButtons}>
                                <Button
                                    title="Create Internal User"
                                    color="#7722dd"
                                    onPress={onCreateInternalUser}
                                />
                                <Button
                                    title="Create OAuth User"
                                    color="#7722dd"
                                    onPress={onCreateOAuthUser}
                                />
                            </View>

                        </View>
                    ) : null
                }
                {authVisible ?
                    (<TextInput style={styles.textInput} placeholder="Username" value={internalUsername} onChangeText={setInternalUsername} autoCapitalize='none' autoCorrect={false}></TextInput>) : null}
                {authVisible ?
                    (<TextInput style={styles.textInput} placeholder="Password" value={internalPassword} onChangeText={setInternalPassword} autoCapitalize='none' autoCorrect={false} secureTextEntry={true}></TextInput>) : null}
                {authVisible ?
                     (<Pressable style={styles.button} onPress={onLoginPress}>
                        <Text style={styles.text}>Login</Text>
                     </Pressable>) : null
                }
                {(uiType == 'suite') ?
                    (<ScrollView ref={ref => { // @ts-ignore
                        this.scrollView = ref}}
                    // @ts-ignore
                     onContentSizeChange={() => this.scrollView.scrollToEnd({animated: true})}>
                        <Text style={styles.transcript}>{transcript}</Text>
                     </ScrollView>) : null
                }
            </View>
        </RootSiblingParent>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    marginVertical: 80,
      rowGap: 20
  },
  text: {
    color: 'lightgrey',
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'center',
    backgroundColor: 'transparent',
  },
  button: {
    backgroundColor: '#0091FF',
    color: 'white',
    marginVertical: 8,
    marginHorizontal: 120,
    borderRadius: 4
  },
  modeButton: {
    backgroundColor: '#79D785',
    color: 'white',
    marginVertical: 8,
    marginHorizontal: 10,
    paddingHorizontal: 20,
    borderRadius: 4
  },
  transcript: {
    backgroundColor: 'white',
    color: 'black',
    fontSize: 14,
    marginHorizontal: 10
  },
  textInput: {
    borderColor: 'lightgrey',
    borderWidth: 1,
    marginTop: 10,
    borderRadius: 4,
    color: 'black',
    marginHorizontal: 80,
    height: 40,
    fontSize: 16,
  },
    navButtons: {
        flexDirection: 'row',
        columnGap: 16
    },
    container1: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        rowGap: 20
    },
});
