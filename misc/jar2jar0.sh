#!/bin/bash

function jar2jar0
{
        tmpdir=`mktemp $MKTEMP_FLAGS`
        jar=$1
        jar0=$2
        cd $tmpdir && jar xf $jar && {
                for i in `find . -name \*.jar`; do
                        echo "           `basename $i`..."
                        ( jar2jar0 $tmpdir/$i $tmpdir/$i.0 && mv $i.0 $i ) ||
                        ( echo "$0: ERROR"; exit 1 )
                done
		jar cM0fm $jar0 META-INF/MANIFEST.MF . &&
                        cd - >/dev/null 2>&1 && rm -rf $tmpdir && return 0
        }
	return 1
}

case `uname -s` in
Linux)	MKTEMP_FLAGS=-d
	;;
*)	MKTEMP_FLAGS="-d /tmp/jar2jar0.XXXXXX"
	;;
esac

echo "Converting `basename $1`..."
jar2jar0 $1 $2

