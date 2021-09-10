This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

## Prerequisites

Before you can run this web app, you need a running Keycloak service. For this, you can either start
a real Keycloak server:

```bash
./gradlew example-integration-docker:realKeycloakComposeUp
```

The server has a pre-configured user `user` with password `user` in realm `realm` and
client `client`. The client has enabled authorization code flow, implicit flow and direct access
password grant flow.

Stopping the server can be done running:

```bash
./gradlew example-integration-docker:realKeycloakComposeDown
```

Or you can start our standalone server:

```bash
./gradlew example-integration-docker:composeUp
```

Stopping:

```bash
./gradlew example-integration-docker:composeDown
```

## Run the application

In the project directory, you can run:

`./yarn start`

This runs the app in the development mode.<br>
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

The page will reload if you make edits.<br>
You will also see any lint errors in the console.
