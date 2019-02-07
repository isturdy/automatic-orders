#!/bin/bash

# just so nothing weird happens
set -eu

mkdir -p releases

MOD=$(basename "$(pwd)")
VERSION=$(grep version mod/mod_info.json | grep -o '\d\.\d\.\d')
RELEASE=$MOD-$VERSION

# copy over mod to release folder
cp -R mod releases/$RELEASE
cp -R src releases/$RELEASE

pushd releases
pushd $RELEASE
zip -r src.zip src
rm -r src
popd
zip -r $RELEASE.zip $RELEASE
popd
