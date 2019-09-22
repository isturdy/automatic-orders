#!/usr/bin/env bash

starsector_dir="/Applications/Starsector.app"
starsector_core_dir="${starsector_dir}/Contents/Resources/Java"
mod_name="${PWD##*/}"
ln -s "${starsector_core_dir}/starfarer.api.jar" "${PWD}/libs/starfarer/starfarer-api/1.0/starfarer-api-1.0.jar"
ln -s "${starsector_core_dir}/starfarer.api.zip" "${PWD}/libs/starfarer/starfarer-api/1.0/starfarer-api-1.0-sources.jar"
ln -s  "${PWD}/mod" "${starsector_dir}/mods/${mod_name}"
