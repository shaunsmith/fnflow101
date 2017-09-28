#!/bin/bash
set -x
set -e
DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' functions)

fn apps c flow101 || true
fn apps config set flow101 COMPLETER_BASE_URL http://$DOCKER_LOCALHOST:8081
fn apps config set flow101 SLACK_API_TOKEN  $SLACK_API_TOKEN
fn apps config set flow101 SLACK_CHANNEL  $FLOW101_SLACK_CHANNEL

pushd functions
  fn deploy --app flow101 --local --all
popd

fn routes l flow101

fn apps i flow101