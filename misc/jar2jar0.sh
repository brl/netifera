#!/bin/bash

function jar2jar0
{
        tmpdir=`mktemp -d`
        jar=$1
        jar0=$2
        cd $tmpdir && jar xf $jar && {
                for i in `find . -name \*.jar`; do
                        echo "           `basename $i`..."
                        ( jar2jar0 $tmpdir/$i $tmpdir/$i.0 && mv $i.0 $i ) ||
                        ( echo "$0: ERROR"; exit 1 )
                done

                # Some bundles will not work if the files are not placed
                # in some special order (?) (f.ex: kf's declarative services)
                files=`jar tf $jar | grep -v META-INF`
                zip -qr0 $jar0 META-INF/MANIFEST.MF $files META-INF &&
                        cd - >/dev/null 2>&1 && rm -rf $tmpdir && return 0
        }
        exit 1
}

echo "Converting `basename $1`..."
jar2jar0 $1 $2
