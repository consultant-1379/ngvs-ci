echo "Open http://127.0.0.1:5050 for testing dsl"
cd job-dsl-playground/
PATH="${PWD}/gradle-2.4/bin:$PATH" RATPACK_OPTS="-Dratpack.port=5050" && build/install/ratpack/bin/ratpack build/install/ratpack/ratpack.groovy