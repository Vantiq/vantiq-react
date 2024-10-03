
import { StyleSheet, View, Button } from 'react-native';
import { init, multiply, add, authWithInternal, authWithOAuth } from 'vantiq-react';


import { useEffect } from 'react';

export default function App() {

  var authenticationState:any = null;

  useEffect(() => {
    // Code to run on component mount
    console.log("INITIALIZING Vantiq Interface");

    let server:string = "http://10.0.0.208:8080";
    let namespace:string = "Scratch1";

    server = "https://test.vantiq.com";
    namespace = "SteveNS1";

    init(server,namespace).then(function(authState:any)
        {
          authenticationState = authState;
          let auth:string = JSON.stringify(authState,null,3)
          console.log(`init: server=${server} namespace=${namespace} return=${auth}`);
        },
        function(error:any)
        {
            let err:string = JSON.stringify(error,null,3);
            console.log(`init FAIL: ${err}`);
        });

    // Optional cleanup function
    return () => {
      console.log("UNMOUNTING Vantiq Interface");
    };
  }, []); //


  const onAdd = () => {
    let a:number = 5;
    let b:number = 7;

    console.log('Invoke Add');

    add(a,b).then(function(value:number)
     {
       console.log(`Add ${a} + ${b} = ${value}`);
     },
     function(err:string)
     {
       console.log(`Error ${err}`);
     });
  };

  const onMultiply = () => {
    let a:number = 2;
    let b:number = 6;

    console.log('Invoke Multiply');

    multiply(a,b).then(function(value:number)
        {
          console.log(`Multiply ${a} * ${b} = ${value}`);
        },
        function(err:string)
        {
          console.log(`Error ${err}`);
        });
  };


  const onValidate= () => {

    console.log('Invoke onValidate');

    if (authenticationState.authValid)
    {
      console.log("Validation: current access token valid")
    }
    //
    //  Either (1) there was no current token, (2) the current token is not valid, (3) the server/namespace changed
    //
    else
    {
      if (authenticationState.serverType == "internal")
      {
        let username:string = "steve1";
        let password:string = "x"

        console.log("Validation: INTERNAL")

        authWithInternal(username,password).then(
          function(newAuthState:any)
          {
              authenticationState = newAuthState;
              let auth:string = JSON.stringify(authenticationState,null,3)
              console.log(`Validation: authValid=${authenticationState.authValid} authState=${auth}`);
          },
          function(error:any)
          {
            console.log(`Validation INTERNAL REJECT error=${JSON.stringify(error)}`)
          }
        );

      }
      else if (authenticationState.serverType == "oauth")
      {
        console.log("Validation: OAUTH");

        authWithOAuth("redirectURL","clientId").then(
          function(newAuthState:any)
          {
            authenticationState = newAuthState;
            let auth:string = JSON.stringify(authenticationState,null,3)
            console.log(`Validation: authValid=${authenticationState.authValid} authState=${auth}`);
          },
          function(error:any)
          {
            console.log(`Validation OAUTH REJECT error=${JSON.stringify(error)}`);
          }
        );
      }
      else
      {
        console.log(`Validation FAIL: serverType invalid=${authenticationState.serverType}`);
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
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    rowGap:20
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
