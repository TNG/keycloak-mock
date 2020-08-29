<!--
  ~ Copyright 2016 Red Hat, Inc. and/or its affiliates
  ~ and other contributors as indicated by the @author tags.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<html>
<body>
<script>
    var init;

    function checkState(clientId, origin, sessionState, callback) {
        var cookie = getCookie();

        var checkCookie = function() {
            if (clientId === init.clientId && origin === init.origin) {
                var c = cookie.split('/');
                if (sessionState === c[2]) {
                    callback('unchanged');
                } else {
                    callback('changed');
                }
            } else {
                callback('error');
            }
        }

        if (!init) {
            var req = new XMLHttpRequest();

            var url = location.href.split("?")[0] + "/init";
            url += "?client_id=" + encodeURIComponent(clientId);
            url += "&origin=" + encodeURIComponent(origin);

            req.open('GET', url, true);

            req.onreadystatechange = function () {
                if (req.readyState === 4) {
                    if (req.status === 204 || req.status === 1223) {
                        init = {
                            clientId: clientId,
                            origin: origin
                        }
                        if (!cookie) {
                            if (sessionState != '') {
                                callback('changed');
                            } else {
                                callback('unchanged');
                            }
                        } else {
                            checkCookie();
                        }
                    } else {
                        callback('error');
                    }
                }
            };

            req.send();
        } else  if (!cookie) {
            if (sessionState != '') {
                callback('changed');
            } else {
                callback('unchanged');
            }
        } else {
            checkCookie();
        }
    }

    function getCookie()
    {
        var cookie = getCookieByName('KEYCLOAK_SESSION');
        if (cookie === null) {
            return getCookieByName('KEYCLOAK_SESSION_LEGACY');
        }
        return cookie;
    }

    function getCookieByName(name)
    {
        name = name + '=';
        var ca = document.cookie.split(';');
        for(var i=0; i<ca.length; i++)
        {
            var c = ca[i].trim();
            if (c.indexOf(name)===0) return c.substring(name.length,c.length);
        }
        return null;
    }

    function receiveMessage(event)
    {
        if (typeof event.data !== 'string') {
            return
        }

        var origin = event.origin;
        var data = event.data.split(' ');
        if (data.length != 2) {
            return;
        }

        var clientId = data[0];
        var sessionState = data[1];

        checkState(clientId, event.origin, sessionState, function(result) {
            event.source.postMessage(result, origin);
        });
    }

    window.addEventListener("message", receiveMessage, false);
</script>
</body>
</html>
