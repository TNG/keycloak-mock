This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

## Prerequisites

Before you can run this web app, you need a running Keycloak service.
For this, you can either start a real Keycloak server with the provided configuration file
`realm.json` (which will create a user `user` with password `user`), e.g. with:

```bash
docker run -p 8000:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin \
    -e KEYCLOAK_IMPORT=/tmp/realm.json -v $(pwd)/realm.json:/tmp/realm.json \
    jboss/keycloak:9.0.0
```

Or you can start our standalone server with no need for additional configuration:

```bash
cd ..
./gradlew standalone:run
```

## Run the application

In the project directory, you can run:

`./yarn start`

This runs the app in the development mode.<br>
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

The page will reload if you make edits.<br>
You will also see any lint errors in the console.
