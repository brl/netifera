for i in *.jar; do ../../misc/jar2jar0.sh `pwd`/$i `pwd`/$i.stored && mv $i.stored $i; done
