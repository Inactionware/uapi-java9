rm -rf out
mkdir out

mkdir out/classes
javac -d out/classes --module-source-path src/main/java -m uapi.common --module-path

mkdir out/mods
jar -cf out/mods/uapi.common.jar -C out/classes/uapi.common .