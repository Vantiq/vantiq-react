import React, {useEffect, useState} from 'react'
import { NativeModules, StyleSheet, Pressable, Text, View, TextInput, ScrollView, Button } from "react-native";
import { RootSiblingParent } from 'react-native-root-siblings';

import {init, authWithInternal, authWithOAuth, select, selectOne, count,
  insert, update, upsert, deleteOne, deleteWhere, execute, publishEvent, verifyAuthToken} from 'vantiq-react';

const {VantiqReact} = NativeModules;
const VANTIQ_SERVER:string = 'https://staging.vantiq.com';
const VANTIQ_NAMESPACE:string = 'react'
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
        VantiqReact.init(VANTIQ_SERVER, VANTIQ_NAMESPACE).then(
          function(response) {
             authenticationState = response;
             if (response.authValid) {
                 setStartVisible(true);
             } else {
                 // authentication error so need to authenticate
                 if (response.serverType == VantiqInternal) {
                     setAuthVisible(true);
                 } else if (response.serverType == VantiqOAuth) {
                     VantiqReact.authWithOAuth(OAUthUrlScheme, OAuthClientId).then(
                        function(response) {
                           if (response.authValid) {
                               setStartVisible(true);
                           } else {
                               addToTranscript('Authentication error: ' + response.errorStr);
                           }
                        }, function(error) {
                           addToTranscript('Authentication error: ' + response.errorStr);
                        });
                 } else {
                     addToTranscript('Invalid server type');
                 }
             }
           }, function(error) {
             addToTranscript('init fail: ' + response.errorStr);
           });
    }, []);

    // user presses the Start Running button
    function onButtonPress() {
        setStartVisible(false);
        runTests();
    }
    
    // user presses one of the Test Mode buttons
    function onModePress(mode) {
        setUIType(mode);
    }
    
    // used for logging in to an Internal auth server, after the user has entered
    // a username and password (internalUsername, internalPassword)
    function onLoginPress() {
        setAuthVisible(false);
        VantiqReact.authWithInternal(internalUsername, internalPassword).then(
            function(response) {
               if (response.authValid) {
                   runTests();
               } else {
                   addToTranscript('Authentication error: ' + response.errorStr);
                   setAuthVisible(true);
               }
            }, function(error) {
                setAuthVisible(true);
                addToTranscript('Authentication error: ' + error.message + ", code: " + error.code);
            });
    }
    
    // helper to add text to the suite transcript
    addToTranscript = function(text) {
        transcriptText += text + '\n';
        setTranscript(transcriptText);
    }
    
    // helper to add any error string and code to the transcript
    reportTestError = function(op, error) {
        addToTranscript(op + ' error: ' + error.message + ", code: " + error.code);
    }
    
    // test harness for running in suite mode
    let lastVantiqID:string;
    runTests = function() {
        setRunningSuite(true);
        VantiqReact.select('system.types', [], null, {}, -1).then(
            function(data) {
                let length:number = data ? data.length : "N/A";
                addToTranscript('Select returns ' + length + ' items.');
            }, function(error) {
                reportTestError('Select', error);
            });
        setTimeout(function() {
            VantiqReact.count('system.types', {"ars_version":{"$gt":5}}).then(
                 function(count) {
                     addToTranscript('Count returns ' + count + ' items.');
                 }, function(error) {
                     reportTestError('Count', error);
                 });
        }, 2000);
        setTimeout(function() {
            VantiqReact.insert('TestType', {"intValue":42,"uniqueString":"42", boolValue:true}).then(
                function(data) {
                    data = data ? data : {};
                    lastVantiqID = (data._id).toString();
                    addToTranscript('Insert successful, id: ' + lastVantiqID + '.');
                }, function(error) {
                    reportTestError('Insert', error);
                });
        }, 4000);
        setTimeout(function() {
            VantiqReact.upsert('TestType', {"intValue":44,"uniqueString":"A Unique String."}).then(
                function(data) {
                    addToTranscript('Upsert successful.');
                }, function(error) {
                    reportTestError('Upsert', error);
                });
        }, 6000);
        setTimeout(function() {
            VantiqReact.upsert('TestType', {"intValue":45,"uniqueString":"A Unique String."}).then(
                function(data) {
                    addToTranscript('Upsert successful.');
                }, function(error) {
                    reportTestError('Upsert', error);
                });
        }, 8000);
        setTimeout(function() {
            VantiqReact.update('TestType', lastVantiqID, {"stringValue":"Updated String."}).then(
                function(data) {
                    addToTranscript('Update successful.');
                }, function(error) {
                    reportTestError('Update', error);
                });
        }, 10000);
        setTimeout(function() {
            VantiqReact.selectOne('TestType', lastVantiqID).then(
                function(data) {
                    data = data ? data : [];
                    addToTranscript('SelectOne successful: ' + data.length + ' records.');
                }, function(error) {
                    reportTestError('SelectOne', error);
                });
        }, 12000);
        setTimeout(function() {
            VantiqReact.publish('/vantiq', {"intValue":42}).then(
                function(data) {
                    addToTranscript('Publish successful.');
                }, function(error) {
                    reportTestError('Publish', error);
                });
        }, 14000);
        setTimeout(function() {
            //VantiqReact.execute('sumTwo', [35, 21]).then(
            VantiqReact.execute('sumTwo', {"val1":35, "val2":21}).then(
                function(data) {
                    addToTranscript('Execute successful: ' + JSON.stringify(data));
                }, function(error) {
                    reportTestError('Execute', error);
                });
        }, 16000);
        setTimeout(function() {
            VantiqReact.deleteOne('TestType', lastVantiqID).then(
                function(data) {
                    addToTranscript('DeleteOne successful.');
                }, function(error) {
                    reportTestError('DeleteOne', error);
                });
        }, 18000);
        setTimeout(function() {
            VantiqReact.delete('TestType', {"intValue":42}).then(
                function(data) {
                    addToTranscript('Delete successful.');
                }, function(error) {
                    reportTestError('Delete', error);
                });
        }, 20000);
        setTimeout(function() {
            VantiqReact.select('system.types', ["name", "_id"], {}, {"name":-1}, -1).then(
                function(data) {
                    let length:number = data ? data.length : "N/A";
                    addToTranscript('Select returns ' + length + ' items.');
                }, function(error) {
                    reportTestError('Select', error);
                });
        }, 22000);
        setTimeout(function() {
            VantiqReact.delete('TestType', {"intValue":42}).then(
                function(data) {
                    addToTranscript('Delete successful.');
                    addToTranscript('Tests complete.');
                    setStartVisible(true);
                    setRunningSuite(false);
                }, function(error) {
                    reportTestError('Delete', error);
                    addToTranscript('Tests complete.');
                    setStartVisible(true);
                    setRunningSuite(false);
                });
        }, 24000);
    }
    
    // methods for running in single mode
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
    }
    const onRefresh = () => {

      console.log('Invoke onRefresh');

      authenticationState.authValid = false;

      if (authenticationState.serverType == VantiqInternal)
      {
        console.error(`Can't Refresh Internal`);

      }
      else if (authenticationState.serverType == VantiqOAuth)
      {
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

    }
    const onSelect = () => {
      let type: string = "a.b.c.MyType";
      let props: string[] = ["aaa", "bbb", "ccc", "qqqq", "MyBoolean", "MyObject", "stringAry"];
      let where: any = {
        ccc:"c"
      };
      let sortSpec: any = {
        bbb:1
      };
      let limit: number = 100;

      where = {};
      sortSpec = {};

      console.log('Invoke Select');

      select(type, props, where, sortSpec, limit).then(function (results: any) {
            console.log(`select results=${JSON.stringify(results, null, 3)}`)
          },
          function (error: any) {
            console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
          })
    }
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
    }
    const onCount = () => {
      let type: string = "a.b.c.MyType";
      let where: any = {
        ccc:"c"
      };

      where = null;

      console.log('Invoke Count');

      count(type,where).then(function (results: any) {
            console.log(`select results=${JSON.stringify(results, null, 3)}`)
          },
          function (error: any) {
            console.error(`select REJECT error=${JSON.stringify(error, null, 3)}`)
          })
    }
    const onInsert = () => {
      let type: string = "a.b.c.MyType";
      let object: any = {
        MyBoolean: false,
        ccc: "CCCC INSERT",
        bbb: "BBB INSERT " + Math.random(),
        aaa: "AAAA INSERT"
      };

      console.log('Invoke Insert');

      insert(type,object).then(function (results: any) {
            console.log(`insert results=${JSON.stringify(results, null, 3)}`)
          },
          function (error: any) {
            console.error(`insert REJECT error=${JSON.stringify(error, null, 3)}`)
          })
    }
    const onPublish = () => {
      let resourceId: string = "a.b.c.MyService/VEH";
      let resource:string = "services";
      let object: any = {
        ccc: "CCCC",
        bbb: "BBBB",
        aaa: "AAAA"
      };

      console.log('Invoke Publish');

      publishEvent(resource,resourceId,object).then(function (results: any) {
            console.log(`publishEvent results=${JSON.stringify(results, null, 3)}`)
          },
          function (error: any) {
            console.error(`publishEvent REJECT error=${JSON.stringify(error, null, 3)}`)
          })
    }
    const onUpsert = () => {
      let type: string = "a.b.c.MyType";
      let object: any = {
        MyBoolean: false,
        bbb: "NATKEY",
        aaa: "AAAA UPDATE " + Math.random()
      };

      console.log('Invoke Upsert');

      upsert(type,object).then(function (results: any) {
            console.log(`upsert results=${JSON.stringify(results, null, 3)}`)
          },
          function (error: any) {
            console.error(`upsert REJECT error=${JSON.stringify(error, null, 3)}`)
          })
    }
    const onUpdate = () => {
      let type: string = "a.b.c.MyType";
      let object: any = {
        MyBoolean: false,
        ccc: "CCCC UPDATE " + Math.random()
      };
      let id:string = "6700304b033ebc6020bf1f2f";

      console.log('Invoke Update1');

      update(type,id,object).then(function (results: any) {
            console.log(`update results=${JSON.stringify(results, null, 3)}`)
          },
          function (error: any) {
            console.error(`update REJECT error=${JSON.stringify(error, null, 3)}`)
          })
    }
    const onDelete = () => {
      let type: string = "a.b.c.MyType";
      // @ts-ignore
      let where: string = {
        bbb: "b2"
      };

      console.log('Invoke Select');

      deleteWhere(type,  where).then(function (results: any) {
            console.log(`delete results=${JSON.stringify(results, null, 3)}`)
          },
          function (error: any) {
            console.error(`delete REJECT error=${JSON.stringify(error, null, 3)}`)
          })
    }
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
    }
    const onExecute = () => {
      let procedureName: string = "TestProc";
      let params: any = {
        aaa: 4,
        bbb: 6
      };
        params = [6, 4];

      console.log('Invoke Execute');

      //procedureName = "NoParms";
      //paramsAsString = null;

      execute(procedureName,  params).then(function (results: any) {
            console.log(`execute results=${JSON.stringify(results, null, 3)}`)
          },
          function (error: any) {
            console.error(`execute REJECT error=${JSON.stringify(error, null, 3)}`)
          })
    }
    
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
                    (<View>
                         <Button title="Validate" color="#00ffff" onPress={onValidate}/>
                         <Button title="Refresh" color="#aa0000" onPress={onRefresh}/>
                         <Button title="Select" color="#880088" onPress={onSelect}/>
                         <Button title="SelectOne" color="#cc00cc" onPress={onSelectOne} />
                         <Button title="Count" color="#220022" onPress={onCount}/>
                         <Button title="Insert" color="#338833" onPress={onInsert}/>
                         <Button title="Update" color="#882288" onPress={onUpdate}/>
                         <Button title="Upsert" color="#6622dd" onPress={onUpsert}/>
                         <Button title="Delete" color="#6622dd" onPress={onDelete}/>
                         <Button title="Delete One" color="#6622dd" onPress={onDeleteOne}/>
                         <Button title="Execute" color="#2255dd" onPress={onExecute}/>
                         <Button title="Publish" color="#3388aa" onPress={onPublish}/>
                     </View>) : null
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
                    (<ScrollView ref={ref => {this.scrollView = ref}}
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
    marginHorizontal: 140,
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
    height: 30,
    fontSize: 16,
  },
});
