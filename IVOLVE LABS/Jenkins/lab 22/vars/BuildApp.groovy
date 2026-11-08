def call() {
    sh '''
        npm install
        if node -e "process.exit(require('./package.json').scripts && require('./package.json').scripts.test ? 0 : 1)"; then
            npm test
        else
            echo "No npm test script configured; skipping tests."
        fi
    '''
}
