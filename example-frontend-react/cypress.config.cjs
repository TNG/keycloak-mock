const {defineConfig} = require('cypress')

module.exports = defineConfig({
  chromeWebSecurity: false,
  allowCypressEnv: false,
  e2e: {},
})
