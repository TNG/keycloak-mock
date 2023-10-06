import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import Keycloak from 'keycloak-js';
import axios from 'axios';

const kc = new Keycloak('/keycloak.json');

kc.init({onLoad: 'login-required', promiseType: 'native', enableLogging: true})
  .then(authenticated => {
    if (authenticated) {
      ReactDOM.render(<App logout={() => kc.logout()}/>, document.getElementById('root'));
    }
  });

axios.interceptors.request.use(config => (
  kc.updateToken(5)
    .then(_ => {
      config.headers.Authorization = 'Bearer ' + kc.token;
      return Promise.resolve(config)
    })
    .catch(() => {
      kc.login();
    })
));
