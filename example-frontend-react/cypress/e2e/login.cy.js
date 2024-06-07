describe('Keycloak mock standalone', () => {
  beforeEach(() => {
    cy.login({
      root: 'http://localhost:8000',
      realm: 'realm',
      username: 'user123',
      password: 'vip',
      client_id: 'client',
      redirect_uri: 'http://localhost:8080/index.html'
    })
  })
  afterEach(() => {
    cy.logout({
      root: 'http://localhost:8000',
      realm: 'realm',
      redirect_uri: 'http://localhost:8080/index.html'
    })
  })
  describe('authorization code flow', () => {
    it('works', () => {
      cy.visit('http://localhost:8080/index.html')

      cy.contains('Hello User123')
    })
  })
})
