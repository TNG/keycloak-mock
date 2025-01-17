<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Documentation</title>
  <link rel="stylesheet" href="/style.css">
</head>
<body>
<h1>Keycloak Mock API</h1>
<p>These are the endpoints that are currently supported by Keycloak Mock.</p>
<table>
  <tr>
    <th>Methods</th>
    <th>Path</th>
    <th>Description</th>
  </tr>
  <#list descriptions as description>
    <tr>
      <td>${description.methods()?join(", ")}</td>
      <td>${description.getPath()}</td>
      <td>${description.getName()}</td>
    </tr>
  </#list>
</table>
</body>
</html>
