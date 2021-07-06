#!/bin/bash

set CURDIR=%~dp0

java -cp %CURDIR%bin;%CURDIR%lib\jargs.jar;%CURDIR%lib\jargs-test.jar tests.OPTester %*

