def call() {
    sh '''
        set -eu
        npm install
        npm test
        node --check server.js
    '''
}
