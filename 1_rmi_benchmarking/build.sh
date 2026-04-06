#!/bin/bash
echo "Cleaning..."
rm -rf build dist
mkdir build dist

echo "Copying external JARs..."
cp external/*.jar dist/

echo "Compiling Java files..."
javac -d build -cp "dist/*" src/*/*/*.java

echo "Creating JAR..."
jar cf dist/dspa1.jar -C build .

echo "Build complete!"

