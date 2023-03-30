#!/bin/sh

FILE=jenkins-error-log

grep -E 'Connection refused|timed out|UnknownHostException' "$FILE" && (echo 'network error'; exit 1;)
grep -E 'Permission denied|No space left on device|Too many open files|Out of memory' "$FILE" && (echo 'resource error'; exit 1;)
grep -E 'JAVA_HOME not set' "$FILE" && (echo 'environment error'; exit 1;)
grep -E 'Invalid argument|Invalid input' "$FILE" && (echo 'argument error'; exit 1;)
exit 0;