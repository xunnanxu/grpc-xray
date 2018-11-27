#!/usr/bin/env bash
set -exo pipefail
if [[ ${TRAVIS_BRANCH} = release/* ]] && [[ ${TRAVIS_PULL_REQUEST} = 'false' ]]; then
    openssl aes-256-cbc -K $encrypted_549eb73a0bc0_key -iv $encrypted_549eb73a0bc0_iv -in signing.asc.enc -out signing.asc -d
    ${GPG_EXECUTABLE} --fast-import signing.asc
    mvn -B -U -P ossrh -DskipTests=true --settings .travis-settings.xml deploy
    git status
    mvn gitflow:release-finish
fi
