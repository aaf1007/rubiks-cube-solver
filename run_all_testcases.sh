#!/bin/bash

# Compile Java files
echo "Compiling Java files..."
javac -d . src/rubikscube/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Compilation successful!"
echo ""

# Create output directory
mkdir -p outputs

# Run all test cases
for i in {01..40}; do
    input="testcases-new/scramble${i}.txt"
    output="outputs/output${i}.txt"
    
    if [ -f "$input" ]; then
        echo "Running test case $i..."
        java rubikscube.Solver "$input" "$output"
        echo "---"
    fi
done

echo ""
echo "All tests complete! Results saved in outputs/ directory"
