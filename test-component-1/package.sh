#!/bin/bash

# echo "This is a temporary script for test-component-1"

# echo "Checking if release '${{env.TAG}}' exists"
# export tag=${{env.TAG}}
tag="test-component-1-1.0.3"
# echo $tag
if gh release view $tag &>/dev/null; then
# echo "::error::Release '${{env.TAG}}' already exists"
echo "exists"
else
echo "Starting release"
fi