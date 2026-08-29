import React from 'react';
import {createRoot} from 'react-dom/client';
import './index.css';
import App from './App';
import Keycloak from 'keycloak-js';
import axios from 'axios';

const kc = new Keycloak('/keycloak.json');

kc.init({onLoad: 'login-required',enableLogging: true})
  .then(authenticated => {
    if (authenticated) {
      createRoot(document.getElementById('root')).render(<App logout={() => kc.logout()}/>);
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
