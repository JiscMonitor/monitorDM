#!/bin/bash

curl -H "Accept: application/json" 'http://doaj.org/search?source=\{query:\{query_string:\{query:Science,default_operator:AND\}\}\}'
