<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Login</title>
</head>
<body>
<h1>Keycloak Mock</h1>
<p>This is a mocked login screen. Instead of providing a password, you can enter a comma-separated list of roles your user is supposed to
  have.</p>
<form action="/authenticate" id="authenticate">
  <p>
    <label for="user">User</label>
    <br>
    <input type="text" name="user" id="user">
  </p>

  <p>
    <label for="roles">Roles</label>
    <br>
    <input type="text" name="roles" id="roles">
  </p>

  <input type="hidden" name="realm" id="realm" value="${realm}">
  <input type="hidden" name="state" id="state" value="${state}">
  <input type="hidden" name="nonce" id="nonce" value="${nonce}">
  <input type="hidden" name="session_id" id="session_id" value="${session_id}">
  <input type="hidden" name="client_id" id="client_id" value="${client_id}">
  <input type="hidden" name="redirect_uri" id="redirect_uri" value="${redirect_uri}">

  <button type="submit">Login</button>
</form>
</body>
</html>
