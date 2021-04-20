<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Login</title>
</head>
<body>
<h1>Keycloak Mock</h1>
<p>This is a mocked login screen. Instead of providing a password, you can enter a comma-separated
  list of roles your user is supposed to have.</p>
<form action="${authentication_uri}" id="authenticate" method="post">
  <p>
    <label for="username">User</label>
    <br>
    <input type="text" name="username" id="username">
  </p>

  <p>
    <label for="password">Roles</label>
    <br>
    <input type="text" name="password" id="password">
  </p>

  <button type="submit">Login</button>
</form>
</body>
</html>
